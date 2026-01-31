package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.phase.impl.DiscoveryPhase;
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
import java.util.Set;
import java.util.regex.Pattern;

public record HorizonPluginMetadata(
    String name,
    String version,
    List<EntrypointObject> entrypoints,
    List<String> transformers,
    List<String> authors,
    Type pluginType,
    boolean loadDatapackEntry,
    List<String> mixins,
    List<String> wideners,
    List<InstanceInteraction> interactions,
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
        if (DiscoveryPhase.PAPER_SPIGOT_PL_STORAGE.containsKey(name)) {
            throw new IllegalStateException("Duplicate plugin ID found: " + name);
        }
        final String version = root.getValueOrThrow("version").asString();
        final List<String> authors = new ArrayList<>(
            root.getArrayOptional("authors")
                .map((arr) -> arr.asList(String.class))
                .orElse(List.of())
        );
        root.getValueSafe("author").asStringOptional().ifPresent(authors::add);
        final Type type = root.getValueOrThrow("type").as(Type.class);

        // optional arguments now
        List<EntrypointObject> entrypoints = root.getArrayOptional("entrypoints")
            .map((arr) -> arr.asList(EntrypointObject.class))
            .orElse(new ArrayList<>());

        if (!type.equals(Type.HORIZON)) {
            entrypoints.stream()
                .filter((eo) -> eo.key().equalsIgnoreCase("plugin_main"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("If declared a Spigot or Paper plugin definition, must include 'plugin_main' entrypoint"));
        }

        List<String> transformers = root.getArrayOptional("transformers")
            .map((arr) -> arr.asList(String.class))
            .orElse(new ArrayList<>());
        boolean loadDatapackEntry = root.getValueSafe("load_datapack_entry").asBooleanOptional().orElse(false);

        List<String> mixins = root.getArrayOptional("mixins")
            .map((arr) -> arr.asList(String.class))
            .orElse(new ArrayList<>());
        List<String> wideners = root.getArrayOptional("wideners")
            .map((arr) -> arr.asList(String.class))
            .orElse(new ArrayList<>());

        List<InstanceInteraction> interactions = root.getArrayOptional("interactions")
            .map((arr) -> arr.asList(InstanceInteraction.class))
            .orElse(new ArrayList<>());

        return new HorizonPluginMetadata(
            name, version, entrypoints, transformers, authors, type, loadDatapackEntry, mixins, wideners, interactions,
            new NestedData(new HashSet<>(), new HashSet<>(), new HashSet<>())
        );
    };

    public static final MappedTypeConverter<InstanceInteraction.Resolver, String> INTERACTION_RESOLVER_CONVERTER = (final String val) -> {
        return InstanceInteraction.Resolver.valueOf(val.toUpperCase());
    };
    public static final MappedTypeConverter<InstanceInteraction.Type, String> INTERACTION_TYPE_CONVERTER = (final String val) -> {
        return InstanceInteraction.Type.valueOf(val.toUpperCase());
    };
    public static final MappedTypeConverter<Type, String> PLUGIN_TYPE_CONVERTER = (final String val) -> {
        return Type.valueOf(val.toUpperCase());
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
    public static final TypeConverter<InstanceInteraction> INTERACTION_CONVERTER = (final Object val) -> {
        final ObjectTree root = (ObjectTree) val;
        final InstanceInteraction.Type type = root.getValueOrThrow("type").as(InstanceInteraction.Type.class);
        return new InstanceInteraction(
            switch (type) {
                case SPIGOT_PLUGIN -> InteractionParser.SPIGOT_PLUGIN.parse(root);
                case PAPER_PLUGIN -> InteractionParser.PAPER_PLUGIN.parse(root);
                case MINECRAFT -> InteractionParser.MINECRAFT.parse(root);
                case JAVA -> InteractionParser.JAVA.parse(root);
                case ASM -> InteractionParser.ASM.parse(root);
            },
            root.getValueOrThrow("interacts").as(InstanceInteraction.Resolver.class)
        );
    };

    public String encodeToYaml() {
        if (pluginType.equals(Type.HORIZON)) {
            throw new IllegalStateException("Cannot run this on horizon plugin meta");
        }
        ObjectTree.Builder builder = ObjectTree.builder();
        builder
            .put("name", name)
            .put("version", version)
            .put("api-version", HorizonLoader.getInstance().getVersionMeta().minecraftVersion().getId())
            .put("authors", authors.toArray(new String[0]));
        for (final EntrypointObject entrypoint : entrypoints) {
            if (entrypoint.key().equalsIgnoreCase("plugin_main")) {
                builder.put("main", entrypoint.clazz());
            }
            else {
                builder.put(entrypoint.key(), entrypoint.clazz());
            }
        }
        try {
            return ObjectTree.write(builder.build()).format(Format.YAML).asString();
        } catch (WriteException e) {
            throw new RuntimeException("Unable to write to YAML", e);
        }
    }

    public record NestedData(
        Set<Pair<FileJar, HorizonPluginMetadata>> horizonEntries,
        Set<FileJar> serverPluginEntries,
        Set<FileJar> libraryEntries
    ) {}
}
