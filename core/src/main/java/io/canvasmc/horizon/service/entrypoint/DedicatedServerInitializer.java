package io.canvasmc.horizon.service.entrypoint;

@EntrypointHandler(value = "onInitialize", argTypes = {})
public interface DedicatedServerInitializer {
    void onInitialize();
}
