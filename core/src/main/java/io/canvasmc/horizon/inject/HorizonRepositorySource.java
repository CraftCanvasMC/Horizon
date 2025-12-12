package io.canvasmc.horizon.inject;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Acts as a repository source for horizon plugins marked as embedding
 * datapack entries
 *
 * @author dueris
 */
public record HorizonRepositorySource(DirectoryValidator validator) implements RepositorySource {
    private static final TaggedLogger LOGGER = Logger.tag("datapack-injection");
    private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);

    private static @NonNull String nameFromPath(@NonNull Path path) {
        return path.getFileName().toString();
    }

    @Override
    public void loadPacks(@NonNull Consumer<Pack> onLoad) {
        try {
            discoverPacks(this.validator, (path, resources) -> {
                PackLocationInfo packLocationInfo = this.createDiscoveredFilePackInfo(path);
                Pack metaAndCreate = Pack.readMetaAndCreate(packLocationInfo, resources, PackType.SERVER_DATA, DISCOVERED_PACK_SELECTION_CONFIG);
                if (metaAndCreate != null) {
                    onLoad.accept(metaAndCreate);
                }
            });
        } catch (IOException var3) {
            LOGGER.warn("Failed to list horizon injected packs", var3);
        }
    }

    private @NonNull PackLocationInfo createDiscoveredFilePackInfo(Path path) {
        String string = nameFromPath(path);
        return new PackLocationInfo("injected/" + string, Component.literal(string), PackSource.WORLD, Optional.empty());
    }

    public void discoverPacks(DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        PluginPackDetector pluginPackDetector = new PluginPackDetector(validator);

        // read from plugins, not folder, because technically we can include submodules in horizon jars // TODO - actually do this
        for (HorizonPlugin plugin : Horizon.INSTANCE.getPlugins()) {
            if (!plugin.pluginMetadata().loadDatapackEntry()) {
                continue;
            }
            Path path = plugin.file().ioFile().toPath().toAbsolutePath();
            try {
                List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                Pack.ResourcesSupplier resourcesSupplier = pluginPackDetector.detectPackResources(path, list);
                if (!list.isEmpty()) {
                    LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(path, list));
                } else if (resourcesSupplier != null) {
                    LOGGER.debug("Loading injected datapack entry: {}", path.getFileName());
                    output.accept(path, resourcesSupplier);
                } else {
                    LOGGER.debug("Found non-pack entry '{}', ignoring", path);
                }
            } catch (IOException var10) {
                LOGGER.warn("Failed to read properties of '{}', ignoring", path, var10);
            }
        }
    }

    public class PluginPackDetector extends PackDetector<Pack.ResourcesSupplier> {
        public PluginPackDetector(DirectoryValidator validator) {
            super(validator);
        }

        @Override
        protected Pack.@Nullable ResourcesSupplier createZipPack(@NonNull Path path) {
            FileSystem fileSystem = path.getFileSystem();
            if (fileSystem != FileSystems.getDefault() && !(fileSystem instanceof LinkFileSystem)) {
                LOGGER.info("Can't open pack archive at {}", path);
                return null;
            } else {
                return new FilePackResources.FileResourcesSupplier(path);
            }
        }

        @Override
        protected Pack.ResourcesSupplier createDirectoryPack(@NonNull Path path) {
            return new PathPackResources.PathResourcesSupplier(path);
        }

        @Override
        public Pack.@Nullable ResourcesSupplier detectPackResources(@NonNull Path path, @NonNull List<ForbiddenSymlinkInfo> forbiddenSymlinkInfos) throws IOException {

            BasicFileAttributes fileAttributes;
            try {
                fileAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException var6) {
                return null;
            }

            if (fileAttributes.isSymbolicLink()) {
                HorizonRepositorySource.this.validator.validateSymlink(path, forbiddenSymlinkInfos);
                if (!forbiddenSymlinkInfos.isEmpty()) {
                    return null;
                }

                path = Files.readSymbolicLink(path);
                fileAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            }

            return fileAttributes.isRegularFile() && !fileAttributes.isDirectory() && path.getFileName().toString().endsWith(".jar") ? this.createZipPack(path) : null;
        }
    }
}
