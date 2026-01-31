package io.canvasmc.horizon.inject.mixin.plugins;

import io.papermc.paper.plugin.entrypoint.EntrypointHandler;
import io.papermc.paper.plugin.provider.source.FileProviderSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.jar.JarFile;

@Mixin(FileProviderSource.class)
public class FileProviderSourceMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "registerProviders(Lio/papermc/paper/plugin/entrypoint/EntrypointHandler;Ljava/nio/file/Path;)V", at = @At("HEAD"), cancellable = true)
    public void horizon$ignoreHorizonPlugins(final EntrypointHandler entrypointHandler, final Path context, final CallbackInfo ci) throws Exception {
        try (JarFile file = new JarFile(context.toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
            if (file.getEntry("horizon.plugin.json") != null &&
                (file.getEntry("plugin.yml") == null && file.getEntry("paper-plugin.yml") == null)) {
                LOGGER.info("Found Horizon plugin, {}, ignoring", file.getName());
                ci.cancel();
            }
        }
    }
}
