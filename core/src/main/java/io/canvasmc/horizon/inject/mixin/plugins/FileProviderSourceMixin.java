package io.canvasmc.horizon.inject.mixin.plugins;

import io.papermc.paper.SparksFly;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.EntrypointHandler;
import io.papermc.paper.plugin.provider.source.FileProviderSource;
import io.papermc.paper.plugin.provider.type.PluginFileType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.jar.JarFile;

@Mixin(FileProviderSource.class)
public class FileProviderSourceMixin {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private Function<Path, String> contextChecker;

    /**
     * @author dueris
     * @reason ignore horizon plugins
     */
    @Overwrite
    public void registerProviders(EntrypointHandler entrypointHandler, Path context) throws Exception {
        String source = this.contextChecker.apply(context);

        JarFile file = new JarFile(context.toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
        PluginFileType<?, ?> type = PluginFileType.guessType(file);
        if (type == null) {
            // Throw IAE wrapped in RE to prevent callers from considering this a "invalid parameter" as caller ignores IAE.
            if (file.getEntry("META-INF/versions.list") != null) {
                throw new RuntimeException(new IllegalArgumentException(context + " appears to be a server jar! Server jars do not belong in the plugin folder."));
            }

            if (file.getEntry("horizon.plugin.json") != null) {
                LOGGER.info("Found Horizon plugin, {}, ignoring", file.getName());
                return;
            }

            throw new RuntimeException(
                new IllegalArgumentException(source + " does not contain a " + String.join(" or ", PluginFileType.getConfigTypes()) + "! Could not determine plugin type, cannot load a plugin from it!")
            );
        }

        final PluginMeta config = type.getConfig(file);
        if ((config.getName().equals("spark") && config.getMainClass().equals("me.lucko.spark.bukkit.BukkitSparkPlugin")) && !SparksFly.isPluginPreferred()) {
            LOGGER.info("The spark plugin will not be loaded as this server bundles the spark profiler.");
            return;
        }

        type.register(entrypointHandler, file, context);
    }
}
