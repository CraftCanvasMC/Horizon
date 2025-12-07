package io.canvasmc.horizon.util;

import com.google.gson.*;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;

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

    public static final class PaperclipVersionSerializer implements JsonSerializer<PaperclipVersion> {

        @Override
        public @NonNull JsonElement serialize(@NonNull PaperclipVersion src, Type typeOfSrc, @NonNull JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();

            obj.addProperty("id", src.id());
            obj.addProperty("name", src.name());
            obj.addProperty("world_version", src.world_version());
            obj.addProperty("series_id", src.series_id());
            obj.addProperty("protocol_version", src.protocol_version());
            obj.add("pack_version", ctx.serialize(src.pack_version()));
            obj.addProperty("build_time", src.build_time());
            obj.addProperty("java_component", src.java_component());
            obj.addProperty("java_version", src.java_version());
            obj.addProperty("stable", src.stable());
            obj.addProperty("use_editor", src.use_editor());

            return obj;
        }
    }

    public static final class PaperclipVersionDeserializer implements JsonDeserializer<PaperclipVersion> {

        @Override
        public @NonNull PaperclipVersion deserialize(@NonNull JsonElement json, Type typeOfT, @NonNull JsonDeserializationContext ctx)
            throws JsonParseException {

            JsonObject obj = json.getAsJsonObject();

            PaperclipVersion.PackVersion pack = ctx.deserialize(
                obj.get("pack_version"),
                PaperclipVersion.PackVersion.class
            );

            return new PaperclipVersion(
                obj.get("id").getAsString(),
                obj.get("name").getAsString(),
                obj.get("world_version").getAsInt(),
                obj.get("series_id").getAsString(),
                obj.get("protocol_version").getAsInt(),
                pack,
                obj.get("build_time").getAsString(),
                obj.get("java_component").getAsString(),
                obj.get("java_version").getAsInt(),
                obj.get("stable").getAsBoolean(),
                obj.get("use_editor").getAsBoolean()
            );
        }
    }
}
