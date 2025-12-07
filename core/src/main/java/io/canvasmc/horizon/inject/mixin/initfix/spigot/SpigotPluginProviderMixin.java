package io.canvasmc.horizon.inject.mixin.initfix.spigot;

import io.canvasmc.horizon.inject.access.PluginClassloaderHolder;
import io.canvasmc.horizon.plugin.EntrypointLoader;
import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpigotPluginProvider.class)
public class SpigotPluginProviderMixin implements PluginClassloaderHolder {

    @Unique
    private ClassLoader horizon$spigotPluginClassLoader;

    @Override
    public ClassLoader horizon$getPluginClassLoader() {
        return horizon$spigotPluginClassLoader;
    }

    @Override
    public ClassLoader horizon$setPluginClassLoader(ClassLoader loader) {
        return this.horizon$spigotPluginClassLoader = loader;
    }

    @Inject(method = "createInstance()Lorg/bukkit/plugin/java/JavaPlugin;", at = @At(value = "INVOKE", target = "Lorg/bukkit/plugin/java/PluginClassLoader;<init>(Ljava/lang/ClassLoader;Lorg/bukkit/plugin/PluginDescriptionFile;Ljava/io/File;Ljava/io/File;Ljava/lang/ClassLoader;Ljava/util/jar/JarFile;Lio/papermc/paper/plugin/provider/entrypoint/DependencyContext;)V"))
    public void horizon$storeTemporaryRef(CallbackInfoReturnable<JavaPlugin> cir) {
        EntrypointLoader.ACTIVE_PLUGIN_PROVIDER_REF.set(this);
    }
}
