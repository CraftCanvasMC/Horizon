package io.canvasmc.horizon.inject.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MinecraftServer.class)
public class MinecraftServerMixin {

    @ModifyReturnValue(method = "getServerModName", at = @At("RETURN"))
    public String horizon$changeName(String original) {
        return "horizon";
    }
}
