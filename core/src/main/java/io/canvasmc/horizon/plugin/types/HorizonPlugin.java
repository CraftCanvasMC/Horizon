package io.canvasmc.horizon.plugin.types;

import com.google.common.collect.ImmutableList;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.util.FileJar;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a full valid and parsed Horizon plugin data
 * <p>
 * <b>Note:</b> this guarantees all provided values are correct, except it does not validate contents of some files, as
 * this is validated at a later time in the boot process
 * </p>
 *
 * @author dueris
 */
public final class HorizonPlugin {
    private final FileJar file;
    private final HorizonPluginMetadata pluginMetadata;
    private final CompiledNestedPlugins nestedData;

    private FileSystem fileSystem;

    /**
     * Constructs a new Horizon plugin
     *
     * @param file
     *     the IO file and Jar file pair associated with this plugin
     * @param pluginMetadata
     *     the plugin metadata, storing things like the name, version, mixins, etc
     * @param nestedData
     *     all nested data entries in this plugin tree
     */
    public HorizonPlugin(FileJar file, HorizonPluginMetadata pluginMetadata, CompiledNestedPlugins nestedData) {
        this.file = file;
        this.pluginMetadata = pluginMetadata;
        this.nestedData = nestedData;
    }

    /**
     * Get the IO file and Jar file pair associated with this plugin
     *
     * @return the file pair for this plugin
     */
    public FileJar file() {
        return file;
    }

    /**
     * Get the plugin metadata for this plugin
     *
     * @return the metadata
     */
    public HorizonPluginMetadata pluginMetadata() {
        return pluginMetadata;
    }

    /**
     * Get the nested data for this plugin
     *
     * @return the nested data
     */
    public CompiledNestedPlugins nestedData() {
        return nestedData;
    }

    /**
     * Get the file system for the plugin file
     *
     * @return a file system for this plugin
     */
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

    /**
     * Get the absolute path of the jar file associated with this plugin
     *
     * @return the path of the jar
     */
    public @NonNull Path getPath() {
        return file().ioFile().toPath().toAbsolutePath();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HorizonPlugin) obj;
        return Objects.equals(this.pluginMetadata, that.pluginMetadata);
    }

    /**
     * A bundle of nested horizon plugins, server plugins, and libraries
     *
     * @param horizonEntries
     *     horizon plugins
     * @param serverPluginEntries
     *     server plugins
     * @param libraryEntries
     *     library plugins
     *
     * @author dueris
     */
    public record CompiledNestedPlugins(
        List<HorizonPlugin> horizonEntries, List<FileJar> serverPluginEntries,
        List<FileJar> libraryEntries) {

        /**
         * Constructs a new compiled set of nested plugin entries
         *
         * @param horizonEntries
         *     nested horizon plugins
         * @param serverPluginEntries
         *     nested Paper/Spigot plugins
         * @param libraryEntries
         *     nested libraries
         */
        public CompiledNestedPlugins(List<HorizonPlugin> horizonEntries, List<FileJar> serverPluginEntries, List<FileJar> libraryEntries) {
            this.horizonEntries = ImmutableList.copyOf(horizonEntries);
            this.serverPluginEntries = ImmutableList.copyOf(serverPluginEntries);
            this.libraryEntries = ImmutableList.copyOf(libraryEntries);
        }

        /**
         * Gets all the {@link io.canvasmc.horizon.util.FileJar}s for the server plugin entries and horizon plugin
         * entries
         *
         * @return all plugin entries in the nested data
         */
        public @NonNull List<FileJar> allPlugins() {
            List<FileJar> paper = new ArrayList<>(serverPluginEntries);
            horizonEntries.stream()
                .map(HorizonPlugin::file)
                .forEach(paper::add);
            return paper;
        }
    }
}
