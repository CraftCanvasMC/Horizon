package io.canvasmc.testplugin;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.service.entrypoint.DedicatedServerInitializer;

public class ServerMain implements DedicatedServerInitializer {
    @Override
    public void onInitialize() {
        HorizonLoader.LOGGER.info("Hello from test plugin!");
    }
}
