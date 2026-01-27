package io.canvasmc.horizon.inject.access;

public interface IPluginProvider {
    ClassLoader horizon$getPluginClassLoader();

    void horizon$setPluginClassLoader(ClassLoader loader);
}
