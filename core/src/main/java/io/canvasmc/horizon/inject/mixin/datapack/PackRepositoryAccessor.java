package io.canvasmc.horizon.inject.mixin.datapack;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(PackRepository.class)
public interface PackRepositoryAccessor {
    @Accessor("sources")
    Set<RepositorySource> getSources();

    @Accessor("validator")
    DirectoryValidator getValidator();
}
