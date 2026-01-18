package io.canvasmc.horizon.util;

import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.ParseError;
import io.canvasmc.horizon.util.tree.ParseException;
import org.jspecify.annotations.NonNull;

import java.util.List;

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
    }

    public static final class PaperclipVersionDeserializer implements ObjectDeserializer<PaperclipVersion> {

        @Override
        public @NonNull PaperclipVersion deserialize(@NonNull ObjectTree tree) throws ParseException {
            try {
                ObjectTree pack_version = tree.getTree("pack_version");
                PackVersion pack = new PackVersion(
                    // we use an alias mapping to remap 'resource' -> 'resource_major', and 'data' -> 'data_major'
                    // that way older Minecraft versions won't die when trying to parse this version data
                    pack_version.getValueOrThrow("resource_major").asInt(),
                    pack_version.getValueSafe("resource_minor").asIntOptional().orElse(0),
                    pack_version.getValueOrThrow("data_major").asInt(),
                    pack_version.getValueSafe("data_minor").asIntOptional().orElse(0)
                );

                return new PaperclipVersion(
                    tree.getValueOrThrow("id").asString(),
                    tree.getValueOrThrow("name").asString(),
                    tree.getValueOrThrow("world_version").asInt(),
                    tree.getValueOrThrow("series_id").asString(),
                    tree.getValueOrThrow("protocol_version").asInt(),
                    pack,
                    tree.getValueOrThrow("build_time").asString(),
                    tree.getValueOrThrow("java_component").asString(),
                    tree.getValueOrThrow("java_version").asInt(),
                    tree.getValueOrThrow("stable").asBoolean(),
                    tree.getValueOrThrow("use_editor").asBoolean()
                );
            } catch (Exception exe) {
                String out = tree.toString();
                throw new ParseException(List.of(new ParseError("Couldn't read paperclip version: " + out), new ParseError(exe.getMessage())));
            }
        }
    }
}
