package io.canvasmc.horizon.plugin.phase;

import io.canvasmc.horizon.plugin.LoadContext;

/**
 * The plugin loading phase, internally it is implemented by 3 phases, {@code BUILDER}, {@code DISCOVERY},
 * {@code RESOLUTION}
 *
 * @param <I>
 *     the input generic type
 * @param <O>
 *     the output generic type
 *
 * @author dueris
 */
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
     * Gets the plugin loading phase name, used primarily for debugging
     *
     * @return The name of this phase for logging purposes
     */
    String getName();
}