package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.phase.impl.ResolutionPhase;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.Pair;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.MappedTypeConverter;
import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.TypeConverter;
import io.canvasmc.horizon.util.tree.WriteException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record HorizonPluginMetadata(
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

    public static final ObjectDeserializer<HorizonPluginMetadata> PLUGIN_META_FACTORY = (final ObjectTree root) -> {
        // all required stuff first
        // name, version, authors, type(depending on type, we required plugin_main)
        final String name = root.getValueOrThrow("name").asString();
        if (TAKEN_NAMES.matcher(name.toLowerCase()).matches() || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid name used for plugin meta, " + name);
        }
        if (ResolutionPhase.doesPluginExist(name)) {
            throw new IllegalStateException("Duplicate plugin ID found: " + name);
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

    public record NestedData(
        Set<Pair<FileJar, HorizonPluginMetadata>> horizonEntries,
        Set<FileJar> serverPluginEntries,
        Set<FileJar> libraryEntries
    ) {}
}
