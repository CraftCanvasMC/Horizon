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
    ) {
        @Override
        public int resource_major() {
            if (resource_major == Integer.MIN_VALUE)
                throw new UnsupportedOperationException("'resource_major' isn't included in this Minecraft version");
            return resource_major;
        }

        @Override
        public int resource_minor() {
            if (resource_minor == Integer.MIN_VALUE)
                throw new UnsupportedOperationException("'resource_minor' isn't included in this Minecraft version");
            return resource_minor;
        }
    }

    public static final class PaperclipVersionDeserializer implements ObjectDeserializer<PaperclipVersion> {

        @Override
        public @NonNull PaperclipVersion deserialize(@NonNull ObjectTree tree) {
            ObjectTree packTree = tree.getTree("pack_version");
            PackVersion pack = new PackVersion(
                // resource major/minor as optionals, older Minecraft versions do not have these values
                packTree.getValue("resource_major").asIntOptional().orElse(Integer.MIN_VALUE),
                packTree.getValue("resource_minor").asIntOptional().orElse(Integer.MIN_VALUE),
                packTree.getValue("data_major").asInt(),
                packTree.getValue("data_minor").asInt()
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

    public static final class PackVersionDeserializer implements ObjectDeserializer<PackVersion> {

        @Override
        public @NonNull PackVersion deserialize(@NonNull ObjectTree tree) {
            return new PackVersion(
                tree.getValue("resource_major").asInt(),
                tree.getValue("resource_minor").asInt(),
                tree.getValue("data_major").asInt(),
                tree.getValue("data_minor").asInt()
            );
        }
    }
}
