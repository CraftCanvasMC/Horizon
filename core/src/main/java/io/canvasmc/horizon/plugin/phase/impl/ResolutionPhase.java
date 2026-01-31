package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResolutionPhase implements Phase<Set<Pair<FileJar, HorizonPluginMetadata>>, Set<Pair<FileJar, HorizonPluginMetadata>>> {

    // spigot/paper plugin storage, NAME -> <TYPE, VERSION>
    protected static final List<String> PAPER_SPIGOT_PL_STORAGE = new ArrayList<>();

    public static boolean doesPluginExist(String name) {
        return PAPER_SPIGOT_PL_STORAGE.contains(name);
    }

    @Override
    public Set<Pair<FileJar, HorizonPluginMetadata>> execute(final Set<Pair<FileJar, HorizonPluginMetadata>> input, final LoadContext context) throws PhaseException {
        // TODO - verify horizon dependencies
        return input;
    }

    @Override
    public String getName() {
        return "Resolution";
    }
}
