package io.canvasmc.horizon.inject.mixin.jij;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.FileJar;
import io.papermc.paper.plugin.PluginInitializerManager;
import io.papermc.paper.plugin.provider.source.FileArrayProviderSource;
import io.papermc.paper.plugin.util.EntrypointUtil;
import joptsimple.OptionSet;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.List;

@Mixin(PluginInitializerManager.class)
public class PluginInitializerManagerMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/util/EntrypointUtil;registerProvidersFromSource(Lio/papermc/paper/plugin/provider/source/ProviderSource;Ljava/lang/Object;)V", ordinal = 0))
    private static void horizon$jijInject(@NonNull OptionSet optionSet, CallbackInfo ci) {
        List<File> jijCache = Horizon.INSTANCE.getPlugins().stream()
            .map(HorizonPlugin::nestedData)
            .map(HorizonPlugin.NestedData::nestedSPlugins)
            .flatMap(List::stream)
            .map(FileJar::ioFile)
            .toList();
        EntrypointUtil.registerProvidersFromSource(FileArrayProviderSource.INSTANCE, jijCache.toArray(new File[0]));
    }
}
