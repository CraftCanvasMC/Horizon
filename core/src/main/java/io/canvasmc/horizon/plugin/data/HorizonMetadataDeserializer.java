package io.canvasmc.horizon.plugin.data;

import com.google.gson.*;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class HorizonMetadataDeserializer implements JsonDeserializer<HorizonMetadata> {

    @Override
    public @NonNull HorizonMetadata deserialize(@NonNull JsonElement element, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {

        if (!element.isJsonObject()) {
            throw new JsonParseException("Horizon metadata root must be a JSON object");
        }

        JsonObject root = element.getAsJsonObject();

        String name = getRequiredString(root, "name");
        String version = getRequiredString(root, "version");
        String apiVersion = getRequiredString(root, "api-version");
        String main = getRequiredString(root, "main");

        JsonObject horizon = root.getAsJsonObject("horizon");
        if (horizon == null) {
            throw new IllegalArgumentException("'horizon' key not found!");
        }

        List<String> mixins = getStringArray(horizon, "mixins");
        List<String> wideners = getStringArray(horizon, "access-wideners");

        boolean loadDatapackEntry = getBoolean(horizon, "load-datapack-entry", false);

        return new HorizonMetadata(
            name,
            version,
            apiVersion,
            main,
            mixins,
            wideners,
            loadDatapackEntry
        );
    }

    private String getRequiredString(@NonNull JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            throw new JsonParseException("Missing required field: " + key);
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Field '" + key + "' must be a string");
        }
        return el.getAsString();
    }

    private boolean getBoolean(@NonNull JsonObject obj, String key, boolean def) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;

        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("Field '" + key + "' must be a boolean");
        }
        return el.getAsBoolean();
    }

    private @NonNull List<String> getStringArray(@NonNull JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return List.of();
        }

        if (!el.isJsonArray()) {
            throw new JsonParseException("Field '" + key + "' must be an array of strings");
        }

        List<String> list = new ArrayList<>();
        for (JsonElement item : el.getAsJsonArray()) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Field '" + key + "' contains a non-string element");
            }
            list.add(item.getAsString());
        }
        return list;
    }
}
