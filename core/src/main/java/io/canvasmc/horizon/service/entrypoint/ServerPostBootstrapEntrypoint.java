package io.canvasmc.horizon.service.entrypoint;

/**
 * The entrypoint invoked after Minecraft's bootstrap process completes.
 * <p>
 * The designated key for this entrypoint is {@code server_postbootstrap}.
 *
 * @author veguidev
 */
@EntrypointHandler(value = "onInitialize", argTypes = {})
public interface ServerPostBootstrapEntrypoint {

    /**
     * Runs after the Minecraft bootstrap phase has completed.
     */
    void onInitialize();
}
