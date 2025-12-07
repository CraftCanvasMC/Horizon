package io.canvasmc.horizon.inject.mixin;

import org.bukkit.command.SimpleCommandMap;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleCommandMap.class)
public class SimpleCommandMapMixin {

    @Inject(method = "setDefaultCommands", at = @At("HEAD"), cancellable = true)
    public void horizon$removeReloadCommand(@NonNull CallbackInfo ci) {
        ci.cancel();
    }
}
