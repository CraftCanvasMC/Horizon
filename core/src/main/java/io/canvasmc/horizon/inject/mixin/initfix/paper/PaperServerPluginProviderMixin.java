package io.canvasmc.horizon.inject.mixin.initfix.paper;

import io.canvasmc.horizon.inject.access.PluginClassloaderHolder;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.jar.JarFile;

@Mixin(PaperPluginParent.PaperServerPluginProvider.class)
public abstract class PaperServerPluginProviderMixin implements PluginClassloaderHolder {

    @Unique
    private ClassLoader horizon$paperPluginClassLoader;

    @Shadow
    public abstract JarFile file();

    @Override
    public ClassLoader horizon$getPluginClassLoader() {
        return horizon$paperPluginClassLoader;
    }

    @Override
    public ClassLoader horizon$setPluginClassLoader(ClassLoader loader) {
        return this.horizon$paperPluginClassLoader = loader;
    }
}
