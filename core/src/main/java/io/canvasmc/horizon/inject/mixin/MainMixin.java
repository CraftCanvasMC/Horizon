package io.canvasmc.horizon.inject.mixin;

import io.canvasmc.horizon.service.entrypoint.DedicatedServerInitializer;
import io.canvasmc.horizon.service.entrypoint.EntrypointContainer;
import joptsimple.OptionSet;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main", at = @At("HEAD"))
    private static void horizon$serverMainEntrypoint(OptionSet optionSet, CallbackInfo ci) {
        EntrypointContainer.buildProvider("server_main", DedicatedServerInitializer.class, Void.class).invoke();
    }

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;bootStrap()V", shift = At.Shift.AFTER))
    private static void horizon$serverPostBootstrapEntrypoint(OptionSet optionSet, CallbackInfo ci) {
        EntrypointContainer.buildProvider("server_postbootstrap", DedicatedServerInitializer.class, Void.class).invoke();
    }
}
