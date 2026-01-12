package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.ObjectValue;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class HorizonMetadataDeserializer implements ObjectDeserializer<HorizonMetadata> {

    @Override
    public @NonNull HorizonMetadata deserialize(@NonNull ObjectTree tree) {
        String name = tree.getValue("name").asString();
        String version = tree.getValue("version").asString();
        String apiVersion = tree.getValue("api-version").asString();
        String main = tree.getValue("main").asString();

        ObjectTree horizon = tree.getTreeOptional("horizon")
            .orElseThrow(() -> new IllegalArgumentException("'horizon' key not found!"));

        List<String> mixins = getStringList(horizon, "mixins");
        List<String> wideners = getStringList(horizon, "wideners");
        boolean loadDatapackEntry = horizon.getValueOptional("load-datapack-entry")
            .map(ObjectValue::asBoolean)
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
