package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.util.tree.ObjectTree;

public interface InteractionParser<E extends InteractionParser.Data> {
    InteractionParser<GenericData> MINECRAFT = root -> {
        final String versionPattern = root.getValueOrThrow("version").asString();
        return new GenericData(versionPattern);
    };
    InteractionParser<GenericData> JAVA = root -> {
        final String versionPattern = root.getValueOrThrow("version").asString();
        return new GenericData(versionPattern);
    };
    InteractionParser<GenericData> ASM = root -> {
        final String versionPattern = root.getValueOrThrow("version").asString();
        return new GenericData(versionPattern);
    };
    InteractionParser<PluginData> PAPER_PLUGIN = root -> {
        final String versionPattern = root.getValueOrThrow("version").asString();
        final String name = root.getValueOrThrow("name").asString();
        return new PluginData(versionPattern, name);
    };
    InteractionParser<PluginData> SPIGOT_PLUGIN = root -> {
        final String versionPattern = root.getValueOrThrow("version").asString();
        final String name = root.getValueOrThrow("name").asString();
        return new PluginData(versionPattern, name);
    };

    E parse(ObjectTree root);

    interface Data {}

    record GenericData(String versionPattern) implements Data {}

    record PluginData(String versionPattern, String name) implements Data {}
}
