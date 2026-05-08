package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.Pair;
import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.TypeConverter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Horizon plugin data, providing parsed data Horizon uses internally for various services and implementations
 *
 * @param id
 *     the stable plugin identifier
 * @param provides
 *     additional identifiers this plugin provides
 * @param name
 *     the display name
 * @param description
 *     the description
 * @param version
 *     the version
 * @param entrypoints
 *     the entrypoints registered by this plugin
 * @param transformers
 *     the class transformers registered by this plugin
 * @param authors
 *     the authors
 * @param loadDatapackEntry
 *     if Horizon should load the plugin as a datapack too
 * @param mixins
 *     the registered mixins
 * @param wideners
 *     the registered wideners
 * @param dependencies
 *     the dependencies
 * @param nesting
 *     the nested data of the plugin
 *
 * @author dueris
 * @see io.canvasmc.horizon.plugin.data.HorizonPluginMetadata.NestedData
 */
public record HorizonPluginMetadata(
    String id,
    List<String> provides,
    String name,
    String description,
    String version,
    List<EntrypointObject> entrypoints,
    List<String> transformers,
    List<String> authors,
    boolean loadDatapackEntry,
    List<String> mixins,
    List<String> wideners,
    ObjectTree dependencies,
    NestedData nesting
) {
    private static final Pattern TAKEN_NAMES = Pattern.compile("^(?i)(minecraft|java|asm|horizon|bukkit|mojang|spigot|paper|mixin)$");
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_.-]{1,63}$");

    public static final ObjectDeserializer<HorizonPluginMetadata> PLUGIN_META_FACTORY = (final ObjectTree root) -> {
        // all required stuff first
        // name, version, authors, type(depending on type, we required plugin_main)
        final String name = root.getValueOrThrow("name").asString();
        if (TAKEN_NAMES.matcher(name.toLowerCase()).matches() || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid name used for plugin meta, " + name);
        }
        final String id = validateIdentifier(
            root.getValueSafe("id").asStringOptional().orElseGet(() -> defaultIdForName(name)),
            "id"
        );
        final List<String> provides = new ArrayList<>(
            root.getArrayOptional("provides")
                .map((arr) -> arr.asList(String.class))
                .orElse(List.of())
        );
        final Set<String> seenProvides = new HashSet<>();
        for (final String providedId : provides) {
            final String normalized = validateIdentifier(providedId, "provides");
            if (normalized.equals(id)) {
                throw new IllegalArgumentException("Plugin '" + name + "' cannot provide its own id twice: " + id);
            }
            if (!seenProvides.add(normalized)) {
                throw new IllegalArgumentException("Duplicate provided id '" + normalized + "' declared by plugin '" + name + "'");
            }
        }
        final String version = root.getValueOrThrow("version").asString();
        final List<String> authors = new ArrayList<>(
            root.getArrayOptional("authors")
                .map((arr) -> arr.asList(String.class))
                .orElse(List.of())
        );
        root.getValueSafe("author").asStringOptional().ifPresent(authors::add);

        // optional arguments now
        List<String> transformers = root.getArrayOptional("transformers")
            .map((arr) -> arr.asList(String.class))
            .orElse(new ArrayList<>());

        boolean loadDatapackEntry = root.getValueSafe("load_datapack_entry").asBooleanOptional().orElse(false);
        String description = root.getValueSafe("description").asStringOptional().orElse("");

        List<EntrypointObject> entrypoints = root.getArrayOptional("entrypoints")
            .map((arr) -> arr.asList(EntrypointObject.class))
            .orElse(new ArrayList<>());

        List<String> mixins = root.getArrayOptional("mixins")
            .map((arr) -> arr.asList(String.class))
            .orElse(new ArrayList<>());
        List<String> wideners = root.getArrayOptional("wideners")
            .map((arr) -> arr.asList(String.class))
            .orElse(new ArrayList<>());

        return new HorizonPluginMetadata(
            id,
            List.copyOf(provides),
            name, description, version, entrypoints, transformers, authors,
            loadDatapackEntry, mixins, wideners, root.getTreeOptional("dependencies").orElse(ObjectTree.builder().build()),
            new NestedData(new HashSet<>(), new HashSet<>(), new HashSet<>())
        );
    };

    public static final TypeConverter<EntrypointObject> ENTRYPOINT_CONVERTER = (final Object val) -> {
        final ObjectTree root = (ObjectTree) val;
        final String key = root.keys().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unable to find key for entrypoint"));
        return new EntrypointObject(
            key, root.getValueOrThrow(key).asString(),
            root.getValueSafe("order").asIntOptional().orElse(0)
        );
    };

    @Contract("_, _ -> param1")
    private static @NonNull String validateIdentifier(final @NonNull String identifier, final String fieldName) {
        final String normalized = identifier.toLowerCase(Locale.ROOT);
        if (!normalized.equals(identifier)) {
            throw new IllegalArgumentException("Plugin " + fieldName + " must be lowercase: " + identifier);
        }
        if (!VALID_IDENTIFIER.matcher(identifier).matches() || TAKEN_NAMES.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid plugin " + fieldName + ": " + identifier);
        }
        return identifier;
    }

    private static @NonNull String defaultIdForName(final @NonNull String name) {
        final String normalized = name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_.-]+", "-")
            .replaceAll("^[^a-z]+", "")
            .replaceAll("[-_.]{2,}", "-")
            .replaceAll("[-_.]+$", "");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Unable to derive a valid plugin id from name: " + name);
        }

        return normalized;
    }

    /**
     * Returns all identifiers associated with this plugin. The first identifier is the core plugin id, followed by any
     * provided identifiers.
     */
    public @NonNull @Unmodifiable Set<String> identifiers() {
        final LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add(this.id);
        ids.addAll(this.provides);
        return Set.copyOf(ids);
    }

    /**
     * Nested data, containing nested libraries, server plugins, and Horizon plugins
     *
     * @param horizonEntries
     *     nested Horizon plugins
     * @param serverPluginEntries
     *     nested server plugins
     * @param libraryEntries
     *     nested libraries
     *
     * @author dueris
     */
    public record NestedData(
        Set<Pair<FileJar, HorizonPluginMetadata>> horizonEntries,
        Set<FileJar> serverPluginEntries,
        Set<FileJar> libraryEntries
    ) {}
}
