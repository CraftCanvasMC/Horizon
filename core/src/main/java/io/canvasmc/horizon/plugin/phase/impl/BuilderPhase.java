package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BuilderPhase implements Phase<Set<PluginCandidate>, List<HorizonPlugin>> {
    @Override
    public List<HorizonPlugin> execute(@NonNull Set<PluginCandidate> input, LoadContext context) throws PhaseException {
        List<HorizonPlugin> completed = new ArrayList<>();

        try {
            for (PluginCandidate candidate : input) {
                ObjectTree tree = candidate.metadata().rawData();
                HorizonMetadata horizonMetadata = tree.as(HorizonMetadata.class);
                if (horizonMetadata == null) {
                    throw new PhaseException("Couldn't deserialize horizon metadata for candidate '" + candidate.metadata().name() + "'");
                }
                HorizonPlugin.NestedData newNestedData = new HorizonPlugin.NestedData(
                    execute(candidate.nestedData().horizonEntries(), context),
                    candidate.nestedData().serverPluginEntries().stream().toList(),
                    candidate.nestedData().libraryEntries().stream().toList()
                );
                completed.add(new HorizonPlugin(horizonMetadata.name(), candidate.fileJar(), horizonMetadata, newNestedData));
            }
        } catch (Throwable thrown) {
            throw new PhaseException("Couldn't execute builder phase", thrown);
        }

        return completed;
    }

    @Override
    public String getName() {
        return "Builder";
    }
}
