package io.canvasmc.horizon.util;

import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;

// TODO - javadocs
public record PaperclipVersion(
    String id,
    String name,
    int world_version,
    String series_id,
    int protocol_version,
    PackVersion pack_version,
    String build_time,
    String java_component,
    int java_version,
    boolean stable,
    boolean use_editor
) {

    public record PackVersion(
        int resource_major,
        int resource_minor,
        int data_major,
        int data_minor
    ) {}

    public static final class PaperclipVersionDeserializer implements ObjectDeserializer<PaperclipVersion> {

        @Override
        public @NonNull PaperclipVersion deserialize(@NonNull ObjectTree tree) {
            ObjectTree packTree = tree.getTree("pack_version");
            PackVersion pack = new PackVersion(
                packTree.getValueOptional("resource_major").orElseGet(() -> packTree.getValue("resource")).asInt(),
                packTree.containsKey("resource_minor") ? packTree.getValue("resource_minor").asInt() : 0,
                packTree.getValueOptional("data_major").orElseGet(() -> packTree.getValue("data")).asInt(),
                packTree.containsKey("data_minor") ? packTree.getValue("data_minor").asInt() : 0
            );

            return new PaperclipVersion(
                tree.getValue("id").asString(),
                tree.getValue("name").asString(),
                tree.getValue("world_version").asInt(),
                tree.getValue("series_id").asString(),
                tree.getValue("protocol_version").asInt(),
                pack,
                tree.getValue("build_time").asString(),
                tree.getValue("java_component").asString(),
                tree.getValue("java_version").asInt(),
                tree.getValue("stable").asBoolean(),
                tree.getValue("use_editor").asBoolean()
            );
        }
    }
}
