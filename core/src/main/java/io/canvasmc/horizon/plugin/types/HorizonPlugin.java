package io.canvasmc.horizon.plugin.types;

import com.google.common.collect.ImmutableList;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.util.FileJar;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a full valid and parsed Horizon plugin data
 * <p>
 * <b>Note:</b> this guarantees all provided values are correct, except it does not validate contents of some files, as
 * this is validated at a later time in the boot process
 * </p>
 */
public final class HorizonPlugin {
    private final String identifier;
    private final FileJar file;
    private final HorizonMetadata pluginMetadata;
    private final NestedData nestedData;

    private FileSystem fileSystem;

    public HorizonPlugin(String identifier, FileJar file, HorizonMetadata pluginMetadata, NestedData nestedData) {
        this.identifier = identifier;
        this.file = file;
        this.pluginMetadata = pluginMetadata;
        this.nestedData = nestedData;
    }

    public String identifier() {
        return identifier;
    }

    public FileJar file() {
        return file;
    }

    public HorizonMetadata pluginMetadata() {
        return pluginMetadata;
    }

    public NestedData nestedData() {
        return nestedData;
    }

    public @NonNull FileSystem fileSystem() {
        if (this.fileSystem == null) {
            try {
                this.fileSystem = FileSystems.newFileSystem(this.file.ioFile().toPath(), this.getClass().getClassLoader());
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        return this.fileSystem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HorizonPlugin) obj;
        return Objects.equals(this.identifier, that.identifier);
    }

    /**
     * A bundle of nested horizon plugins, server plugins, and libraries
     *
     * @param horizonEntries      horizon plugins
     * @param serverPluginEntries server plugins
     * @param libraryEntries      library plugins
     */
    public record NestedData(List<HorizonPlugin> horizonEntries, List<FileJar> serverPluginEntries,
                             List<FileJar> libraryEntries) {
        public NestedData(List<HorizonPlugin> horizonEntries, List<FileJar> serverPluginEntries, List<FileJar> libraryEntries) {
            this.horizonEntries = ImmutableList.copyOf(horizonEntries);
            this.serverPluginEntries = ImmutableList.copyOf(serverPluginEntries);
            this.libraryEntries = ImmutableList.copyOf(libraryEntries);
        }

        public @NonNull List<FileJar> allPlugins() {
            List<FileJar> paper = new ArrayList<>(serverPluginEntries);
            horizonEntries.stream()
                .map(HorizonPlugin::file)
                .forEach(paper::add);
            return paper;
        }
    }
}
