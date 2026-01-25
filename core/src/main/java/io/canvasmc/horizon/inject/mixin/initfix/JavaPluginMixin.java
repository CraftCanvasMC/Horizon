package io.canvasmc.horizon.inject.mixin.initfix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.canvasmc.horizon.MixinPluginLoader;
import io.canvasmc.horizon.inject.access.PluginClassloaderHolder;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.canvasmc.horizon.HorizonLoader.LOGGER;

@Mixin(JavaPlugin.class)
public abstract class JavaPluginMixin implements PluginClassloaderHolder {

    @Unique
    // Note: use this if we need to get the actual plugin ClassLoader for other patches
    private ClassLoader horizon$pluginClassLoader;

    @Inject(method = "getPlugin", at = @At("HEAD"))
    private static <T extends JavaPlugin> void horizon$storeGetting(@NonNull Class<T> clazz, CallbackInfoReturnable<T> cir,
                                                                    @Share("fetchingPlugin") @NonNull LocalRef<String> fetchingPlugin) {
        if (clazz.getClassLoader() instanceof ConfiguredPluginClassLoader) return;
        fetchingPlugin.set(clazz.getName());
    }

    @ModifyExpressionValue(method = "getPlugin", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
    private static ClassLoader horizon$returnCorrectClassloader(ClassLoader original,
                                                                @Share("fetchingPlugin") @NonNull LocalRef<String> fetchingPlugin) {
        if (original instanceof ConfiguredPluginClassLoader) return original;
        return ((PluginClassloaderHolder) MixinPluginLoader.MAIN2JAVA_PLUGIN.get(fetchingPlugin.get())).horizon$getPluginClassLoader();
    }

    @Inject(method = "getProvidingPlugin", at = @At("HEAD"))
    private static void horizon$storeGettingProvided(@NonNull Class<?> clazz, CallbackInfoReturnable<JavaPlugin> cir,
                                                     @Share("fetchingProvidedPlugin") @NonNull LocalRef<String> fetchingPlugin) {
        if (clazz.getClassLoader() instanceof ConfiguredPluginClassLoader) return;
        fetchingPlugin.set(clazz.getName());
    }

    @ModifyExpressionValue(method = "getProvidingPlugin", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
    private static ClassLoader horizon$returnCorrectProvidingClassloader(ClassLoader original,
                                                                         @Share("fetchingProvidedPlugin") @NonNull LocalRef<String> fetchingPlugin) {
        if (original instanceof ConfiguredPluginClassLoader) return original;
        return ((PluginClassloaderHolder) MixinPluginLoader.MAIN2JAVA_PLUGIN.get(fetchingPlugin.get())).horizon$getPluginClassLoader();
    }

    @ModifyExpressionValue(method = "<init>()V", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getClassLoader()Ljava/lang/ClassLoader;"))
    public ClassLoader horizon$swapClassloader(ClassLoader original) {
        // technically we can be loaded by the Ember classloader, but we need to trick
        // it here so we init the plugin safely, since it *is* safe
        if (original instanceof ConfiguredPluginClassLoader) {
            return original;
        }
        else {
            // Note: we use getAndSet so we avoid a leak
            PluginProvider runningProvider = (PluginProvider) MixinPluginLoader.ACTIVE_PLUGIN_PROVIDER_REF.getAndSet(null);
            if (runningProvider instanceof PluginClassloaderHolder holder) {
                return horizon$setPluginClassLoader(holder.horizon$getPluginClassLoader());
            }

            throw new RuntimeException("Unable to locate correct class loader for plugin! Found: " + original.getClass()
                .getName());
        }
    }

    @Override
    public ClassLoader horizon$getPluginClassLoader() {
        return this.horizon$pluginClassLoader == null ? ((Object) this).getClass().getClassLoader() : this.horizon$pluginClassLoader;
    }

    @Override
    public ClassLoader horizon$setPluginClassLoader(ClassLoader loader) {
        String clazzName = ((Object) this).getClass().getName();
        LOGGER.info("Set stored horizon ClassLoader for '{}'", clazzName);
        // put the main class name, this, as a mapping to the object
        MixinPluginLoader.MAIN2JAVA_PLUGIN.put(clazzName, this);
        return this.horizon$pluginClassLoader = loader;
    }
}
