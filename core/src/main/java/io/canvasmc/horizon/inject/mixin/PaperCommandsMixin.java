package io.canvasmc.horizon.inject.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.canvasmc.horizon.inject.PluginsCommand;
import io.papermc.paper.command.PaperCommands;
import io.papermc.paper.command.brigadier.CommandRegistrationFlag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;

@Mixin(PaperCommands.class)
public class PaperCommandsMixin {
    @WrapOperation(method = "registerCommands()V", at = @At(value = "INVOKE", target = "Lio/papermc/paper/command/PaperCommands;registerInternalCommand(Lcom/mojang/brigadier/tree/LiteralCommandNode;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/util/Set;)V", ordinal = 1))
    private static void horizon$injectCustomPluginsCommand(
        LiteralCommandNode<CommandSourceStack> node,
        String namespace,
        String description,
        List<String> aliases,
        Set<CommandRegistrationFlag> flags,
        @NonNull Operation<Void> original
    ) {
        original.call(PluginsCommand.create(), namespace, description, aliases, flags);
    }
}
