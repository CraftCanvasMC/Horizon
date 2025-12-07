package io.canvasmc.horizon.inject.mixin.initfix.paper;

import io.canvasmc.horizon.inject.access.PluginClassloaderHolder;
import io.canvasmc.horizon.service.MixinLaunch;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(PaperPluginParent.class)
public class PaperPluginParentMixin {

    @Shadow
    @Final
    private PaperPluginClassLoader classLoader;

    @Inject(method = "createPluginProvider", at = @At("RETURN"))
    public void horizon$storeClassloader(PaperPluginParent.PaperBootstrapProvider provider, @NonNull CallbackInfoReturnable<PaperPluginParent.PaperServerPluginProvider> cir) {
        PaperPluginParent.PaperServerPluginProvider pluginProvider = cir.getReturnValue();
        if (!Arrays.stream(MixinLaunch.getContext().initialGameConnections()).toList().contains(pluginProvider.getSource().toAbsolutePath())) {
            // plugin is not a horizon plugin
            return;
        }
        if (pluginProvider instanceof PluginClassloaderHolder holder) {
            holder.horizon$setPluginClassLoader(this.classLoader);
        }
    }
}
