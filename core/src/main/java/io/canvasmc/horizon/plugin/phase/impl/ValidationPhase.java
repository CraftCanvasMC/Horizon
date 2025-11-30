package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import org.jspecify.annotations.NonNull;
import org.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;

public class ValidationPhase implements Phase<Set<PluginCandidate>, Set<PluginCandidate>> {

    @Override
    public Set<PluginCandidate> execute(@NonNull Set<PluginCandidate> input, LoadContext context) throws PhaseException {
        Set<PluginCandidate> validated = new HashSet<>();

        for (PluginCandidate candidate : input) {
            if (validateCandidate(candidate)) {
                validated.add(candidate);
            } else {
                Logger.warn("Plugin {} failed validation", candidate.metadata().name());
            }
        }

        Logger.debug("Validated {}/{} successful plugins", validated.size(), input.size());
        return validated;
    }

    private boolean validateCandidate(@NonNull PluginCandidate candidate) {
        if (candidate.metadata().name() == null || candidate.metadata().name().trim().isEmpty()) {
            Logger.error("Plugin has invalid name");
            return false;
        }

        if (candidate.metadata().version() == null || candidate.metadata().version().trim().isEmpty()) {
            Logger.error("Plugin has invalid version");
            return false;
        }

        if (!candidate.metadata().rawData().containsKey("api-version")) {
            Logger.error("Plugin doesn't contain api version");
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Validation";
    }
}