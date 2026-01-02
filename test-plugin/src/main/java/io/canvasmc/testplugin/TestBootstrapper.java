package io.canvasmc.testplugin;

import io.canvasmc.horizon.Horizon;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public class TestBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NonNull BootstrapContext context) {
        Horizon.LOGGER.info("ADJFHLSJDHFLSKJDFSDF");
    }
}
