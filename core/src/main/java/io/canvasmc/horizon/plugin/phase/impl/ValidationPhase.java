package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Set;

import static io.canvasmc.horizon.plugin.EntrypointLoader.LOGGER;

public class ValidationPhase implements Phase<Set<PluginCandidate>, Set<PluginCandidate>> {

    @Override
    public Set<PluginCandidate> execute(@NonNull Set<PluginCandidate> input, LoadContext context) throws PhaseException {
        Set<PluginCandidate> validated = new HashSet<>();

        for (PluginCandidate candidate : input) {
            if (!validateCandidate(candidate)) {
                LOGGER.warn("Plugin {} failed validation", candidate.metadata().name());
                continue;
            }

            rebuildNested(candidate, context);
            validated.add(candidate);
        }

        LOGGER.debug("Validated {}/{} successful plugins", validated.size(), input.size());
        return validated;
    }

    private void rebuildNested(@NonNull PluginCandidate candidate, LoadContext context) throws PhaseException {
        Set<PluginCandidate> nested = candidate.nestedData().horizonEntries();
        if (nested.isEmpty()) {
            return;
        }

        Set<PluginCandidate> validatedNested = execute(nested, context);
        nested.clear();
        nested.addAll(validatedNested);
    }


    private boolean validateCandidate(@NonNull PluginCandidate candidate) {
        if (candidate.metadata().name() == null || candidate.metadata().name().trim().isEmpty()) {
            LOGGER.error("Plugin has invalid name");
            return false;
        }

        if (candidate.metadata().version() == null || candidate.metadata().version().trim().isEmpty()) {
            LOGGER.error("Plugin has invalid version");
            return false;
        }

        if (!candidate.metadata().rawData().containsKey("api-version")) {
            LOGGER.error("Plugin doesn't contain api version");
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Validation";
    }
}