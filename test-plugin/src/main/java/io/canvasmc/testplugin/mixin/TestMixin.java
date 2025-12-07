package io.canvasmc.testplugin.mixin;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class TestMixin {
    @Shadow
    @Final
    public static Logger LOGGER;

    @Inject(method = "runServer", at = @At("HEAD"))
    private void test(CallbackInfo ci) {
        LOGGER.info("Hello world from MIXIN!");
    }
}
