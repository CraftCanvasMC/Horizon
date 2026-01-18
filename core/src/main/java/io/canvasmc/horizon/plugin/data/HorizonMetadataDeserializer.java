package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.Value;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class HorizonMetadataDeserializer implements ObjectDeserializer<HorizonMetadata> {

    @Override
    public @NonNull HorizonMetadata deserialize(@NonNull ObjectTree tree) {
        String name = tree.getValueOrThrow("name").asString();
        String version = tree.getValueOrThrow("version").asString();
        String apiVersion = tree.getValueOrThrow("api-version").asString();
        String main = tree.getValueOrThrow("main").asString();

        ObjectTree horizon = tree.getTreeOptional("horizon")
            .orElseThrow(() -> new IllegalArgumentException("'horizon' key not found!"));

        List<String> mixins = getStringList(horizon, "mixins");
        List<String> wideners = getStringList(horizon, "wideners");
        boolean loadDatapackEntry = horizon.getValueOptional("load-datapack-entry")
            .map(Value::asBoolean)
            .orElse(false);

        return new HorizonMetadata(
            name,
            version,
            apiVersion,
            main,
            mixins,
            wideners,
            loadDatapackEntry,
            horizon.getTreeOptional("service")
                .flatMap(ot -> ot.asOptional(PluginServiceProvider.class))
                .orElse(PluginServiceProvider.EMPTY)
        );
    }

    private @NonNull List<String> getStringList(@NonNull ObjectTree tree, String key) {
        return tree.getArrayOptional(key)
            .map(array -> array.asList(String.class))
            .orElse(List.of());
    }
}
