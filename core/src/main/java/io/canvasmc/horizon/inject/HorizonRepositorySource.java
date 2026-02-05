package io.canvasmc.horizon.inject;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Acts as a repository source for horizon plugins marked as embedding datapack entries
 *
 * @param validator
 *     the {@link net.minecraft.world.level.validation.DirectoryValidator} passed to the other
 *     {@link net.minecraft.server.packs.repository.RepositorySource}s when creating the pack repository
 *
 * @author dueris
 */
public record HorizonRepositorySource(DirectoryValidator validator) implements RepositorySource {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "datapack-injection");
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

    public void discoverPacks(DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        PluginPackDetector pluginPackDetector = new PluginPackDetector(validator);

        // read from plugins, not folder, because technically we can include submodules in horizon jars
        for (HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
            if (!plugin.pluginMetadata().loadDatapackEntry()) {
                continue;
            }
            Path path = plugin.file().ioFile().toPath().toAbsolutePath();
            try {
                List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                Pack.ResourcesSupplier resourcesSupplier = pluginPackDetector.detectPackResources(path, list);
                if (!list.isEmpty()) {
                    LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(path, list));
                }
                else if (resourcesSupplier != null) {
                    LOGGER.debug("Loading injected datapack entry: {}", path.getFileName());
                    output.accept(path, resourcesSupplier);
                }
                else {
                    LOGGER.debug("Found non-pack entry '{}', ignoring", path);
                }
            } catch (IOException var10) {
                LOGGER.warn("Failed to read properties of '{}', ignoring", path, var10);
            }
        }
    }

    private @NonNull PackLocationInfo createDiscoveredFilePackInfo(Path path) {
        String string = nameFromPath(path);
        return new PackLocationInfo("injected/" + string, Component.literal(string), PackSource.WORLD, Optional.empty());
    }

    /**
     * The plugin pack detector for Horizon is modeled after the
     * {@link net.minecraft.server.packs.repository.FolderRepositorySource.FolderPackDetector}, which is used in
     * {@link
     * io.canvasmc.horizon.inject.HorizonRepositorySource#discoverPacks(net.minecraft.world.level.validation.DirectoryValidator,
     * java.util.function.BiConsumer)} to detect packs in jar files, where packs are validated/detected based on Horizon
     * plugins which have {@code load_datapack_entry} enabled
     *
     * @author dueris
     */
    public class PluginPackDetector extends PackDetector<Pack.@NonNull ResourcesSupplier> {

        public PluginPackDetector(DirectoryValidator validator) {
            super(validator);
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

        @Override
        protected Pack.@Nullable ResourcesSupplier createZipPack(@NonNull Path path) {
            FileSystem fileSystem = path.getFileSystem();
            if (fileSystem != FileSystems.getDefault() && !(fileSystem instanceof LinkFileSystem)) {
                LOGGER.info("Can't open pack archive at {}", path);
                return null;
            }
            else {
                return new FilePackResources.FileResourcesSupplier(path);
            }
        }

        @Override
        protected Pack.ResourcesSupplier createDirectoryPack(@NonNull Path path) {
            return new PathPackResources.PathResourcesSupplier(path);
        }
    }
}
