package io.canvasmc.horizon.inject.mixin.initfix.paper;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.canvasmc.horizon.HorizonLoader;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.PaperClasspathBuilder;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Mixin(PaperClasspathBuilder.class)
public class PaperClasspathBuilderMixin {

    @Shadow
    @Final
    private PluginProviderContext context;

    @ModifyExpressionValue(method = "buildClassLoader", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/loader/PaperClasspathBuilder;buildLibraryPaths(Z)Ljava/util/List;"))
    public List<Path> horizon$loadPluginLibraries(@NonNull List<Path> original) {
        if (!Arrays.stream(HorizonLoader.getInstance().getLaunchService().getInitialConnections()).toList().contains(context.getPluginSource().toAbsolutePath())) {
            // plugin is not a horizon plugin
            return original;
        }
        // add all library jars to classpath
        for (Path path : original) {
            HorizonLoader.getInstance().getLaunchService().getClassLoader().tryAddToHorizonSystemLoader(path);
        }
        return original;
    }
}
