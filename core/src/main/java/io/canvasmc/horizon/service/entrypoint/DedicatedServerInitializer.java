package io.canvasmc.horizon.service.entrypoint;

/**
 * The main entrypoint for Horizon plugins ran after the {@link io.papermc.paper.ServerBuildInfo} is built.
 * <p>
 * The designated key for this entrypoint is {@code server_main} for registering the entrypoint in your plugin
 *
 * @author dueris
 */
@EntrypointHandler(value = "onInitialize", argTypes = {})
public interface DedicatedServerInitializer {
    /**
     * Runs the server plugin entrypoint
     */
    void onInitialize();
}
