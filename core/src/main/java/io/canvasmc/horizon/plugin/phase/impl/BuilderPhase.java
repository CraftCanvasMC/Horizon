package io.canvasmc.horizon.plugin.phase.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class BuilderPhase implements Phase<Set<PluginCandidate>, List<HorizonPlugin>> {
    @Override
    public List<HorizonPlugin> execute(@NonNull Set<PluginCandidate> input, LoadContext context) throws PhaseException {
        List<HorizonPlugin> completed = new ArrayList<>();

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();

        for (PluginCandidate candidate : input) {
            Map<String, Object> raw = candidate.metadata().rawData();
            HorizonMetadata horizonMetadata = Horizon.GSON.fromJson(
                    gson.toJson(raw),
                    HorizonMetadata.class
            );
            if (horizonMetadata == null) {
                throw new PhaseException("Couldn't deserialize horizon metadata for candidate '" + candidate.metadata().name() + "'");
            }
            completed.add(new HorizonPlugin(horizonMetadata.name(), candidate.fileJar(), horizonMetadata));
        }

        return completed;
    }

    @Override
    public String getName() {
        return "Builder";
    }
}
