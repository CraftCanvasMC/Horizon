package io.canvasmc.horizon.plugin.phase;

import io.canvasmc.horizon.plugin.LoadContext;

public interface Phase<I, O> {

    /**
     * Executes this phase of plugin loading
     *
     * @param input
     *     The input from the previous phase
     * @param context
     *     The plugin loading context
     *
     * @return The output to pass to the next phase
     *
     * @throws PhaseException
     *     if the phase fails
     */
    O execute(I input, LoadContext context) throws PhaseException;

    /**
     * @return The name of this phase for logging purposes
     */
    String getName();
}