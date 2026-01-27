package io.canvasmc.horizon.service.entrypoint;

// TODO - javadocs
@EntrypointHandler(value = "onInitialize", argTypes = {})
public interface DedicatedServerInitializer {
    void onInitialize();
}
