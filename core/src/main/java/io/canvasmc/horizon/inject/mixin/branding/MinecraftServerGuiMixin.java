package io.canvasmc.horizon.inject.mixin.branding;

import io.canvasmc.horizon.HorizonLoader;
import net.minecraft.server.gui.MinecraftServerGui;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

@Mixin(MinecraftServerGui.class)
public class MinecraftServerGuiMixin {

    @Redirect(method = "showFrameFor", at = @At(value = "INVOKE", target = "Ljavax/swing/JFrame;setIconImage(Ljava/awt/Image;)V"))
    private static void horizon$injectLogo(@NonNull JFrame instance, Image image) throws IOException {
        instance.setIconImage(ImageIO.read(Objects.requireNonNull(HorizonLoader.class.getClassLoader().getResourceAsStream("logo.png"))));
    }
}
