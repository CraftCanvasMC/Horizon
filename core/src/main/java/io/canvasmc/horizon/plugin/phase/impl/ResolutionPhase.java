package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.MinecraftVersion;
import io.canvasmc.horizon.util.Pair;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResolutionPhase implements Phase<Set<Pair<FileJar, HorizonPluginMetadata>>, Set<Pair<FileJar, HorizonPluginMetadata>>> {

    private static final Pattern COMPARATOR_PATTERN =
        Pattern.compile("^(>=|<=|>|<|=)?\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_TOKEN_PATTERN = Pattern.compile("(\\d+|[a-zA-Z]+)");
    private static final Set<String> RESERVED_DEPENDENCY_KEYS = Set.of("minecraft", "java", "asm");
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "plugin_resolution");

    private static boolean matchesGenericInteger(@NonNull String constraint, int current) {
        Matcher matcher = COMPARATOR_PATTERN.matcher(constraint.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid ASM version constraint: " + constraint);
        }

        String operator = matcher.group(1);
        int target = Integer.parseInt(matcher.group(2));

        if (operator == null || operator.equals("=")) {
            return current == target;
        }

        return switch (operator) {
            case ">" -> current > target;
            case ">=" -> current >= target;
            case "<" -> current < target;
            case "<=" -> current <= target;
            default -> throw new IllegalStateException("Unhandled operator: " + operator);
        };
    }

    private static boolean matches(
        @NonNull String constraint,
        @NonNull MinecraftVersion currentVersion
    ) {
        Predicate<MinecraftVersion> predicate = parse(constraint);
        return predicate.test(currentVersion);
    }

    private static boolean matchesVersion(@NonNull String constraint, @NonNull String currentVersion) {
        Matcher matcher = COMPARATOR_PATTERN.matcher(constraint.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid plugin version constraint: " + constraint);
        }

        final String operator = matcher.group(1);
        final String versionPart = normalizeVersion(matcher.group(2).trim().toLowerCase(Locale.ROOT));
        final String current = normalizeVersion(currentVersion.trim().toLowerCase(Locale.ROOT));

        if (versionPart.endsWith("*")) {
            final String prefix = versionPart.substring(0, versionPart.length() - 1);
            return current.startsWith(prefix);
        }

        final int comparison = compareVersions(current, versionPart);
        if (operator == null || operator.equals("=")) {
            return comparison == 0;
        }

        return switch (operator) {
            case ">" -> comparison > 0;
            case ">=" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=" -> comparison <= 0;
            default -> throw new IllegalStateException("Unhandled operator: " + operator);
        };
    }

    private static int compareVersions(@NonNull String left, @NonNull String right) {
        final List<String> leftTokens = versionTokens(left);
        final List<String> rightTokens = versionTokens(right);
        final int max = Math.max(leftTokens.size(), rightTokens.size());

        for (int i = 0; i < max; i++) {
            final String leftToken = i < leftTokens.size() ? leftTokens.get(i) : null;
            final String rightToken = i < rightTokens.size() ? rightTokens.get(i) : null;
            final int tokenComparison = compareVersionToken(leftToken, rightToken);
            if (tokenComparison != 0) {
                return tokenComparison;
            }
        }

        return 0;
    }

    /**
     * SemVer build metadata must not affect precedence, so anything after '+' is ignored.
     */
    private static @NonNull String normalizeVersion(@NonNull String version) {
        final int buildMetadataSeparator = version.indexOf('+');
        return buildMetadataSeparator >= 0 ? version.substring(0, buildMetadataSeparator) : version;
    }

    private static int compareVersionToken(final String leftToken, final String rightToken) {
        if (leftToken == null && rightToken == null) {
            return 0;
        }
        if (leftToken == null) {
            return isNumericToken(rightToken) ? compareNumeric("0", rightToken) : 1;
        }
        if (rightToken == null) {
            return isNumericToken(leftToken) ? compareNumeric(leftToken, "0") : -1;
        }

        final boolean leftNumeric = isNumericToken(leftToken);
        final boolean rightNumeric = isNumericToken(rightToken);

        if (leftNumeric && rightNumeric) {
            return compareNumeric(leftToken, rightToken);
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? 1 : -1;
        }

        return leftToken.compareTo(rightToken);
    }

    private static int compareNumeric(@NonNull String left, @NonNull String right) {
        return new BigInteger(left).compareTo(new BigInteger(right));
    }

    private static boolean isNumericToken(final String token) {
        return token != null && !token.isEmpty() && Character.isDigit(token.charAt(0));
    }

    private static @NonNull List<String> versionTokens(@NonNull String version) {
        final Matcher matcher = VERSION_TOKEN_PATTERN.matcher(version);
        final List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static @NonNull Predicate<MinecraftVersion> parse(@NonNull String raw) {
        String input = raw.trim().toLowerCase(Locale.ROOT);

        Matcher matcher = COMPARATOR_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version constraint: " + raw);
        }

        String operator = matcher.group(1);
        String versionPart = matcher.group(2).trim();

        // handle wildcards
        if (versionPart.endsWith("*")) {
            String prefix = versionPart.substring(0, versionPart.length() - 1);
            return v -> v.getId().toLowerCase(Locale.ROOT).startsWith(prefix);
        }

        MinecraftVersion target = MinecraftVersion.fromStringId(versionPart);

        if (operator == null || operator.equals("=")) {
            return v -> v == target;
        }

        return switch (operator) {
            case ">" -> v -> v.isNewerThan(target);
            case ">=" -> v -> v.isNewerThanOrEqualTo(target);
            case "<" -> v -> v.isOlderThan(target);
            case "<=" -> v -> v.isOlderThanOrEqualTo(target);
            default -> throw new IllegalStateException("Unhandled operator: " + operator);
        };
    }

    private static void ensureUniqueIdentifiers(
        final Iterable<Pair<FileJar, HorizonPluginMetadata>> plugins,
        final @NonNull HorizonPluginMetadata internalPlugin
    ) throws PhaseException {
        final Map<String, HorizonPluginMetadata> pluginsByIdentifier = new HashMap<>();
        for (final String identifier : internalPlugin.identifiers()) {
            pluginsByIdentifier.put(identifier, internalPlugin);
        }

        for (final Pair<FileJar, HorizonPluginMetadata> pair : plugins) {
            final HorizonPluginMetadata pluginMetadata = pair.b();
            for (final String identifier : pluginMetadata.identifiers()) {
                final HorizonPluginMetadata previous = pluginsByIdentifier.putIfAbsent(identifier, pluginMetadata);
                if (previous != null) {
                    throw new PhaseException(
                        "Duplicate plugin identifier detected: " + identifier +
                            " (in " + previous.name() + " and " + pluginMetadata.name() + ")"
                    );
                }
            }
        }
    }

    private static @NonNull Map<String, HorizonPluginMetadata> providersByIdentifier(
        final @NonNull Iterable<Pair<FileJar, HorizonPluginMetadata>> plugins,
        final @NonNull HorizonPluginMetadata internalPlugin
    ) {
        final Map<String, HorizonPluginMetadata> providers = new HashMap<>();
        internalPlugin.identifiers().forEach(identifier -> providers.put(identifier, internalPlugin));
        for (final Pair<FileJar, HorizonPluginMetadata> pair : plugins) {
            final HorizonPluginMetadata pluginMetadata = pair.b();
            pluginMetadata.identifiers().forEach(identifier -> providers.put(identifier, pluginMetadata));
        }
        return providers;
    }

    private static boolean matchesRuntimeRequirements(
        final @NonNull HorizonPluginMetadata pluginMetadata,
        final MinecraftVersion currentVersion,
        final int asmVersion
    ) {
        if (pluginMetadata.dependencies().containsKey("minecraft")) {
            final String constraint = pluginMetadata.dependencies().getValueOrThrow("minecraft").asString();
            if (!matches(constraint, currentVersion)) {
                LOGGER.error("Version requirement for plugin {} is not met. Current version, {}, requires, {}",
                    pluginMetadata.name(),
                    currentVersion.getName(),
                    constraint
                );
                return false;
            }
        }

        if (pluginMetadata.dependencies().containsKey("java")) {
            final String javaConstraint = pluginMetadata.dependencies().getValueOrThrow("java").asString();
            if (!matchesGenericInteger(javaConstraint, HorizonLoader.JAVA_VERSION)) {
                LOGGER.error(
                    "Java version requirement for plugin {} is not met. Current Java={}, requires={}",
                    pluginMetadata.name(),
                    HorizonLoader.JAVA_VERSION,
                    javaConstraint
                );
                return false;
            }
        }

        if (pluginMetadata.dependencies().containsKey("asm")) {
            final String asmConstraint = pluginMetadata.dependencies().getValueOrThrow("asm").asString();
            if (!matchesGenericInteger(asmConstraint, asmVersion)) {
                LOGGER.error(
                    "ASM version requirement for plugin {} is not met. Current ASM={}, requires={}",
                    pluginMetadata.name(),
                    asmVersion,
                    asmConstraint
                );
                return false;
            }
        }

        return true;
    }

    private static boolean matchesPluginRequirements(
        final @NonNull HorizonPluginMetadata pluginMetadata,
        final Map<String, HorizonPluginMetadata> providersByIdentifier
    ) {
        for (final Map.Entry<String, String> dependency : pluginDependencyConstraints(pluginMetadata.dependencies()).entrySet()) {
            final String requestedId = dependency.getKey();
            final HorizonPluginMetadata provider = providersByIdentifier.get(requestedId);

            if (provider == null) {
                LOGGER.error(
                    "Plugin dependency for {} is missing. Required plugin={}, requires={}",
                    pluginMetadata.name(),
                    requestedId,
                    dependency.getValue()
                );
                return false;
            }

            if (provider.id().equals(pluginMetadata.id())) {
                LOGGER.error("Plugin {} cannot depend on itself via {}", pluginMetadata.name(), requestedId);
                return false;
            }

            if (!matchesVersion(dependency.getValue(), provider.version())) {
                LOGGER.error(
                    "Plugin dependency version for {} is not met. Dependency={} currentVersion={} requires={}",
                    pluginMetadata.name(),
                    requestedId,
                    provider.version(),
                    dependency.getValue()
                );
                return false;
            }
        }

        return true;
    }

    private static @NonNull Map<String, String> pluginDependencyConstraints(final @NonNull ObjectTree dependencies) {
        final Map<String, String> pluginDependencies = new LinkedHashMap<>();
        for (final String key : dependencies.keys()) {
            if (RESERVED_DEPENDENCY_KEYS.contains(key)) {
                continue;
            }

            final String constraint = dependencies.getValueSafe(key).asStringOptional().orElse(null);
            if (constraint == null) {
                continue;
            }

            pluginDependencies.put(key, constraint);
        }
        return pluginDependencies;
    }

    private static @NonNull List<Pair<FileJar, HorizonPluginMetadata>> orderByDependencies(
        final Iterable<Pair<FileJar, HorizonPluginMetadata>> plugins,
        final HorizonPluginMetadata internalPlugin
    ) throws PhaseException {
        final Map<String, Pair<FileJar, HorizonPluginMetadata>> pluginsById = new LinkedHashMap<>();
        final Map<String, HorizonPluginMetadata> providersByIdentifier = providersByIdentifier(plugins, internalPlugin);
        final Map<String, Integer> indegree = new HashMap<>();
        final Map<String, List<String>> dependents = new HashMap<>();

        for (final Pair<FileJar, HorizonPluginMetadata> pair : plugins) {
            final String pluginId = pair.b().id();
            pluginsById.put(pluginId, pair);
            indegree.put(pluginId, 0);
            dependents.put(pluginId, new ArrayList<>());
        }

        for (final Pair<FileJar, HorizonPluginMetadata> pair : plugins) {
            final HorizonPluginMetadata pluginMetadata = pair.b();
            final Set<String> directDependencyIds = new LinkedHashSet<>();
            for (final String dependencyId : pluginDependencyConstraints(pluginMetadata.dependencies()).keySet()) {
                final HorizonPluginMetadata provider = providersByIdentifier.get(dependencyId);
                if (provider == null || provider.id().equals(internalPlugin.id())) {
                    continue;
                }
                directDependencyIds.add(provider.id());
            }

            indegree.put(pluginMetadata.id(), directDependencyIds.size());
            for (final String providerId : directDependencyIds) {
                dependents.computeIfAbsent(providerId, ignored -> new ArrayList<>()).add(pluginMetadata.id());
            }
        }

        final PriorityQueue<String> ready = new PriorityQueue<>();
        indegree.forEach((pluginId, count) -> {
            if (count == 0) {
                ready.add(pluginId);
            }
        });

        final List<Pair<FileJar, HorizonPluginMetadata>> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            final String providerId = ready.remove();
            ordered.add(pluginsById.get(providerId));

            for (final String dependentId : dependents.getOrDefault(providerId, List.of())) {
                final int updated = indegree.computeIfPresent(dependentId, (ignored, value) -> value - 1);
                if (updated == 0) {
                    ready.add(dependentId);
                }
            }
        }

        if (ordered.size() != pluginsById.size()) {
            final List<String> unresolved = indegree.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
            throw new PhaseException("Circular or unresolved plugin dependencies detected: " + String.join(", ", unresolved));
        }

        return ordered;
    }

    @Override
    public Set<Pair<FileJar, HorizonPluginMetadata>> execute(final @NonNull Set<Pair<FileJar, HorizonPluginMetadata>> input, final LoadContext context) throws PhaseException {
        final Map<String, Pair<FileJar, HorizonPluginMetadata>> remaining = new LinkedHashMap<>();
        final MinecraftVersion currentVersion = HorizonLoader.getInstance().getVersionMeta().minecraftVersion();
        final int ASM_VER = MixinTransformationImpl.ASM_VERSION;
        final HorizonPluginMetadata internalPlugin = HorizonLoader.getInternalPlugin().pluginMetadata();

        for (final Pair<FileJar, HorizonPluginMetadata> pair : input) {
            final HorizonPluginMetadata pluginMetadata = pair.b();
            remaining.put(pluginMetadata.id(), pair);
        }

        ensureUniqueIdentifiers(remaining.values(), internalPlugin);

        boolean changed;
        do {
            changed = false;
            final Map<String, HorizonPluginMetadata> providersByIdentifier = providersByIdentifier(remaining.values(), internalPlugin);

            final List<String> toRemove = new ArrayList<>();
            for (final Map.Entry<String, Pair<FileJar, HorizonPluginMetadata>> entry : remaining.entrySet()) {
                final HorizonPluginMetadata pluginMetadata = entry.getValue().b();
                if (!matchesRuntimeRequirements(pluginMetadata, currentVersion, ASM_VER)) {
                    toRemove.add(entry.getKey());
                    continue;
                }
                if (!matchesPluginRequirements(pluginMetadata, providersByIdentifier)) {
                    toRemove.add(entry.getKey());
                }
            }

            if (!toRemove.isEmpty()) {
                changed = true;
                toRemove.forEach(remaining::remove);
            }
        } while (changed);

        return new LinkedHashSet<>(orderByDependencies(remaining.values(), internalPlugin));
    }

    @Override
    public String getName() {
        return "Resolution";
    }
}
