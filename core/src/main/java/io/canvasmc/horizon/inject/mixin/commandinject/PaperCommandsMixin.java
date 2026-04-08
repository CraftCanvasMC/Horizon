package io.canvasmc.horizon.inject.mixin.commandinject;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.canvasmc.horizon.inject.HorizonPluginsCommand;
import io.papermc.paper.command.PaperCommands;
import io.papermc.paper.command.brigadier.CommandRegistrationFlag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;

@Mixin(PaperCommands.class)
public abstract class PaperCommandsMixin {

    @Shadow
    private static void registerInternalCommand(final LiteralCommandNode<CommandSourceStack> node, final String namespace, final String description, final List<String> aliases, final Set<CommandRegistrationFlag> flags) {
    }

    @Redirect(method = "registerCommands()V", at = @At(value = "INVOKE", target = "Lio/papermc/paper/command/PaperCommands;registerInternalCommand(Lcom/mojang/brigadier/tree/LiteralCommandNode;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/util/Set;)V"))
    private static void horizon$redirectPluginsCommand(final LiteralCommandNode<CommandSourceStack> node, final String namespace, final String description, final @NonNull List<String> aliases, final Set<CommandRegistrationFlag> flags) {
        registerInternalCommand(aliases.contains("pl") ? HorizonPluginsCommand.create() : node, namespace, description, aliases, flags);
    }
}
