package io.canvasmc.horizon.inject.access;

public interface PluginClassloaderHolder {

    ClassLoader horizon$getPluginClassLoader();

    ClassLoader horizon$setPluginClassLoader(ClassLoader loader);
}
