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
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResolutionPhase implements Phase<Set<Pair<FileJar, HorizonPluginMetadata>>, Set<Pair<FileJar, HorizonPluginMetadata>>> {

    private static final Pattern COMPARATOR_PATTERN =
        Pattern.compile("^(>=|<=|>|<|=)?\\s*(.+)$", Pattern.CASE_INSENSITIVE);
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

    @Override
    public Set<Pair<FileJar, HorizonPluginMetadata>> execute(final @NonNull Set<Pair<FileJar, HorizonPluginMetadata>> input, final LoadContext context) throws PhaseException {
        final Set<Pair<FileJar, HorizonPluginMetadata>> output = new HashSet<>();
        final MinecraftVersion currentVersion = HorizonLoader.getInstance().getVersionMeta().minecraftVersion();
        final int ASM_VER = MixinTransformationImpl.ASM_VERSION;

        // TODO - depend on other horizon plugins?
        for (final Pair<FileJar, HorizonPluginMetadata> pair : input) {
            HorizonPluginMetadata pluginMetadata = pair.b();

            if (output.stream()
                .map(Pair::b)
                .map(HorizonPluginMetadata::name)
                .toList().contains(pluginMetadata.name())) {
                // name already contained in output, duplicate names?
                throw new PhaseException("Duplicate plugin name detected: " + pluginMetadata.name());
            }

            // validate Minecraft versions first
            if (pluginMetadata.dependencies().containsKey("minecraft")) {
                String constraint = pluginMetadata.dependencies().getValueOrThrow("minecraft").asString();
                if (!matches(constraint, currentVersion)) {
                    LOGGER.error("Version requirement for plugin {} is not met. Current version, {}, requires, {}",
                        pluginMetadata.name(),
                        currentVersion.getName(),
                        constraint
                    );
                    continue;
                }
            }

            // check Java version
            if (pluginMetadata.dependencies().containsKey("java")) {
                String javaConstraint = pluginMetadata.dependencies()
                    .getValueOrThrow("java")
                    .asString();

                if (!matchesGenericInteger(javaConstraint, HorizonLoader.JAVA_VERSION)) {
                    LOGGER.error(
                        "Java version requirement for plugin {} is not met. Current Java={}, requires={}",
                        pluginMetadata.name(),
                        HorizonLoader.JAVA_VERSION,
                        javaConstraint
                    );
                    continue;
                }
            }

            // check ASM version
            if (pluginMetadata.dependencies().containsKey("asm")) {
                String asmConstraint = pluginMetadata.dependencies().getValueOrThrow("asm").asString();
                if (!matchesGenericInteger(asmConstraint, ASM_VER)) {
                    LOGGER.error(
                        "ASM version requirement for plugin {} is not met. Current ASM={}, requires={}",
                        pluginMetadata.name(),
                        ASM_VER,
                        asmConstraint
                    );
                    continue;
                }
            }

            // validated successfully
            output.add(pair);
        }
        return output;
    }

    @Override
    public String getName() {
        return "Resolution";
    }
}
