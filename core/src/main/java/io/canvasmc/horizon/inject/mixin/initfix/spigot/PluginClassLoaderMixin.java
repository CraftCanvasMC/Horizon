package io.canvasmc.horizon.inject.mixin.initfix.spigot;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.MixinPluginLoader;
import io.canvasmc.horizon.inject.access.IPluginProvider;
import io.papermc.paper.plugin.provider.entrypoint.DependencyContext;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.PluginClassLoader;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;
import java.util.jar.JarFile;

@Mixin(PluginClassLoader.class)
public class PluginClassLoaderMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Constructor;newInstance([Ljava/lang/Object;)Ljava/lang/Object;"))
    public void horizon$swapProviderWithClassLoader(ClassLoader parent, PluginDescriptionFile description, File dataFolder, @NonNull File file, ClassLoader libraryLoader, JarFile jarFile, DependencyContext dependencyContext, CallbackInfo ci) {
        if (!Arrays.stream(HorizonLoader.getInstance().getLaunchService().getInitialConnections()).toList().contains(file.toPath().toAbsolutePath())) {
            // plugin is not a horizon plugin
            return;
        }
        ((IPluginProvider) MixinPluginLoader.ACTIVE_PLUGIN_PROVIDER_REF.get()).horizon$setPluginClassLoader((ClassLoader) (Object) this);
    }

    @ModifyExpressionValue(method = "initialize", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
    public ClassLoader horizon$swapClassLoaderWithThis(ClassLoader original) {
        return (ClassLoader) (Object) this;
    }
}
