package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.Pair;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BuilderPhase implements Phase<Set<Pair<FileJar, HorizonPluginMetadata>>, List<HorizonPlugin>> {

    @Override
    public List<HorizonPlugin> execute(@NonNull Set<Pair<FileJar, HorizonPluginMetadata>> input, LoadContext context) throws PhaseException {
        List<HorizonPlugin> completed = new ArrayList<>();

        try {
            for (Pair<FileJar, HorizonPluginMetadata> pair : input) {
                HorizonPluginMetadata metadata = pair.b();
                FileJar source = pair.a();

                HorizonPlugin.CompiledNestedPlugins newNestedData = new HorizonPlugin.CompiledNestedPlugins(
                    execute(metadata.nesting().horizonEntries(), context),
                    metadata.nesting().serverPluginEntries().stream().toList(),
                    metadata.nesting().libraryEntries().stream().toList()
                );
                completed.add(new HorizonPlugin(source, metadata, newNestedData));
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
