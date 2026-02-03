package io.canvasmc.horizon.inject.mixin.datapack;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.canvasmc.horizon.inject.HorizonRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedList;
import java.util.List;

@Mixin(ServerPacksSource.class)
public class ServerPacksSourceMixin {

    @ModifyReturnValue(method = "createPackRepository(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At("RETURN"))
    private static @NonNull PackRepository horizon$injectPluginSource(@NonNull PackRepository original) {
        List<RepositorySource> modifiedSources = new LinkedList<>(original.sources);
        modifiedSources.addLast(new HorizonRepositorySource(original.validator));
        return new PackRepository(
            original.validator, modifiedSources.toArray(new RepositorySource[0])
        );
    }
}
