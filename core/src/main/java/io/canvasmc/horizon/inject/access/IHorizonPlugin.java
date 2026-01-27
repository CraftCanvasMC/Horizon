package io.canvasmc.horizon.inject.access;

import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;

public interface IHorizonPlugin {
    HorizonPlugin horizon$getHorizonPlugin();

    ConfiguredPluginClassLoader horizon$getConfiguredPluginClassLoader();
}
