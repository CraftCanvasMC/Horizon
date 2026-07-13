package io.canvasmc.horizon.inject.mixin;

import io.canvasmc.horizon.service.entrypoint.EntrypointContainer;
import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootstrapMixin {

    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/entrypoint/LaunchEntryPointHandler;enterBootstrappers()V"))
    private static void horizon$serverBootstrapEntrypoint(final CallbackInfo ci) {
        EntrypointContainer.buildProvider("server_bootstrap", ServerPostBootstrapEntrypoint.class, Void.class).invoke();
    }
}
