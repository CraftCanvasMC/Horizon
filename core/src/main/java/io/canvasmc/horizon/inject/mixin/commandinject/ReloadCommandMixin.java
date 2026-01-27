package io.canvasmc.horizon.inject.mixin.commandinject;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.ReloadCommand;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void horizon$removeReloadCommand(CommandSender sender, String currentAlias, String[] args, @NonNull CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        cir.setReturnValue(true);
        Command.broadcastCommandMessage(sender, Component.text("The reload command is disabled in Horizon", NamedTextColor.RED));
    }
}
