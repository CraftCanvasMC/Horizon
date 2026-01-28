package io.canvasmc.horizon.inject.mixin.plugins.init;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.MixinPluginLoader;
import io.papermc.paper.plugin.entrypoint.strategy.ProviderConfiguration;
import io.papermc.paper.plugin.entrypoint.strategy.modern.ModernPluginLoadingStrategy;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.entrypoint.DependencyContext;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModernPluginLoadingStrategy.class)
public class ModernPluginLoadingStrategyMixin<T> {

    @WrapOperation(method = "loadProviders", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/entrypoint/strategy/ProviderConfiguration;applyContext(Lio/papermc/paper/plugin/provider/PluginProvider;Lio/papermc/paper/plugin/provider/entrypoint/DependencyContext;)V"))
    public void cacheCurrentProvider(ProviderConfiguration instance, PluginProvider<T> tPluginProvider, DependencyContext dependencyContext, @NonNull Operation<Void> original) {
        original.call(instance, tPluginProvider, dependencyContext);
        // Note: bootstrapper works fine, doesn't need specific patch
        if (tPluginProvider instanceof PaperPluginParent.PaperServerPluginProvider paperPluginProvider) {
            MixinPluginLoader.ACTIVE_PLUGIN_PROVIDER_REF.set(paperPluginProvider);
        }
        else if (tPluginProvider instanceof SpigotPluginProvider spigotPluginProvider) {
            MixinPluginLoader.ACTIVE_PLUGIN_PROVIDER_REF.set(spigotPluginProvider);
        }
    }
}
