package io.canvasmc.horizon.inject.mixin.plugins.init;

import com.google.common.base.Preconditions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.MixinPluginLoader;
import io.canvasmc.horizon.inject.access.IHorizonPlugin;
import io.canvasmc.horizon.inject.access.IPluginProvider;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.EmberClassLoader;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(JavaPlugin.class)
public abstract class JavaPluginMixin implements IHorizonPlugin {

    @Unique
    private static final Map<String, JavaPlugin> horizon$fullClassName2javaPlugin = new ConcurrentHashMap<>();

    @Unique
    // Note: use this if we need to get the actual plugin ClassLoader for other patches
    private ConfiguredPluginClassLoader horizon$configuredPluginClassLoader;
    @Unique
    private HorizonPlugin horizon$plugin;

    /**
     * @author dueris
     * @reason fix classloader check to support Horizon plugins
     */
    @Overwrite
    public static <T extends JavaPlugin> T getPlugin(Class<T> clazz) {
        Preconditions.checkArgument(clazz != null, "Null class cannot have a plugin");
        if (!JavaPlugin.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " does not extend " + JavaPlugin.class);
        }

        final ClassLoader cl = clazz.getClassLoader();
        if (cl instanceof ConfiguredPluginClassLoader configuredPluginClassLoader) {
            JavaPlugin plugin = configuredPluginClassLoader.getPlugin();
            if (plugin == null) {
                throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");
            }
            return clazz.cast(plugin);
        }
        else {
            // classloader isn't configured plugin class loader, but is a java plugin class
            // try and see if we can fetch the object itself from Horizon
            if (!horizon$fullClassName2javaPlugin.containsKey(clazz.getName())) {
                throw new IllegalArgumentException("Unable to fetch plugin classloader. Found '" + cl.getClass().getName() + "', expected configured plugin classloader, or ember for plugin class '" + clazz.getName() + "'");
            }
            return clazz.cast(horizon$fullClassName2javaPlugin.get(clazz.getName()));
        }
    }

    /**
     * @author dueris
     * @reason fix classloader check to support Horizon plugins
     */
    @Overwrite
    public static @NonNull JavaPlugin getProvidingPlugin(@Nullable Class<?> clazz) {
        // ok so this one is much more difficult, but essentially what we do here is we
        // try and browse through each plugin and find if the class is associated with ember
        Preconditions.checkArgument(clazz != null, "Null class cannot have a plugin");
        final ClassLoader cl = clazz.getClassLoader();
        if (cl instanceof ConfiguredPluginClassLoader configuredPluginClassLoader) {
            JavaPlugin plugin = configuredPluginClassLoader.getPlugin();
            if (plugin == null) {
                throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");
            }
            return plugin;
        }
        else if (cl instanceof EmberClassLoader ember) {
            // loaded by ember, lets try and locate this
            final String name = clazz.getName();
            final EmberClassLoader.ResourceConnection resourceConnection = ember.getResourceConnection(name);
            if (resourceConnection == null) {
                // if we cannot locate from ember, then it isn't loaded by ember or its parent
                throw new IllegalArgumentException("Unable to locate plugin class from ember class loader");
            }
            try {
                CodeSource codeSource = resourceConnection.source();
                if (codeSource == null) throw new IllegalStateException("CodeSource was null");
                Path path = Path.of(codeSource.getLocation().toURI()).toAbsolutePath();
                HorizonPlugin plugin = HorizonLoader.getInstance().getPlugins().getAll().stream()
                    .filter((pl) -> pl.getPath().equals(path))
                    // find the plugin or throw
                    .findFirst().orElseThrow(() -> new IllegalStateException("Path '" + path + "' was not associated with any Horizon plugins"));
                return getPlugin(Class.forName(plugin.pluginMetadata().main()).asSubclass(JavaPlugin.class));
            } catch (Throwable thrown) {
                throw new RuntimeException("Unable to resolve providing plugin for class: " + name, thrown);
            }
        }
        else {
            throw new IllegalArgumentException("Unable to fetch plugin classloader. Found '" + cl.getClass().getName() + "', expected configured plugin classloader, or ember for class '" + clazz.getName() + "'");
        }
    }

    @Shadow
    public abstract PluginMeta getPluginMeta();

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
            if (runningProvider instanceof IPluginProvider holder) {
                return (ClassLoader) (horizon$configuredPluginClassLoader = (ConfiguredPluginClassLoader) holder.horizon$getPluginClassLoader());
            }

            throw new RuntimeException("Unable to locate correct class loader for plugin! Found: " + original.getClass()
                .getName());
        }
    }

    @Inject(method = "<init>()V", at = @At("RETURN"))
    public void horizon$storeHorizonPlugin(final CallbackInfo ci) {
        HorizonLoader.getInstance().getPlugins().getAll().stream()
            .filter((pl) -> pl.pluginMetadata().name().equalsIgnoreCase(this.getPluginMeta().getName()))
            // only one of each name type can exist/should exist
            .findFirst().ifPresent((pl) -> horizon$plugin = pl);
        horizon$fullClassName2javaPlugin.put(getClass().getName(), (JavaPlugin) (Object) this);
    }

    @Override
    public ConfiguredPluginClassLoader horizon$getConfiguredPluginClassLoader() {
        return horizon$configuredPluginClassLoader;
    }

    @Override
    public HorizonPlugin horizon$getHorizonPlugin() {
        return horizon$plugin;
    }
}
