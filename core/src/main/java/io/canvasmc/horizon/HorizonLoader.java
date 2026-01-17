package io.canvasmc.horizon;

import io.canvasmc.horizon.instrument.JvmAgent;
import io.canvasmc.horizon.instrument.patch.ServerPatcherEntrypoint;
import io.canvasmc.horizon.logger.Level;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.logger.stream.OutStream;
import io.canvasmc.horizon.plugin.EntrypointLoader;
import io.canvasmc.horizon.plugin.PluginTree;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.data.PluginServiceProvider;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.MixinLaunch;
import io.canvasmc.horizon.transformer.AccessTransformationImpl;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.PaperclipVersion;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

/**
 * The main class for Horizon that acts as a base
 * that runs the full startup and bootstrap process
 *
 * @author dueris
 */
public class HorizonLoader {
    public static final boolean DEBUG = Boolean.getBoolean("Horizon.debug");
    public static final Logger LOGGER = Logger.create()
        .name("main")
        .out(OutStream.CONSOLE.allowColors().build())
        .pattern("[{date: HH:mm:ss}] [{level}" + (DEBUG ? "/{class}" : "") + "]: {message}")
        .level(DEBUG ? Level.DEBUG : Level.INFO)
        .build();
    public static HorizonPlugin INTERNAL_PLUGIN;
    public static HorizonLoader INSTANCE;

    private @NonNull
    final ServerProperties properties;
    private @NonNull
    final String version;
    private @NonNull
    final Instrumentation instrumentation;
    private
    final List<Path> initialClasspath;
    private @NonNull
    final FileJar paperclipJar;
    private PluginTree plugins;
    private PaperclipVersion paperclipVersion;

    public HorizonLoader(@NonNull ServerProperties properties, @NonNull String version, @NonNull Instrumentation instrumentation, List<Path> initialClasspath, String @NonNull [] providedArgs) {
        this.properties = properties;
        this.version = version;
        this.instrumentation = instrumentation;
        this.initialClasspath = initialClasspath;

        INSTANCE = this;
        try {
            File paperclipIOFile = properties.serverJar();
            this.paperclipJar = new FileJar(paperclipIOFile, new JarFile(paperclipIOFile));

            File horizonIOFile = Path.of(HorizonLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();

            INTERNAL_PLUGIN = new HorizonPlugin(
                "horizon",
                new FileJar(horizonIOFile, new JarFile(horizonIOFile)),
                new HorizonMetadata(
                    "horizon",
                    this.version,
                    "<void>",
                    "<void>",
                    List.of("internal.mixins.json"),
                    List.of(),
                    false,
                    PluginServiceProvider.DESERIALIZER.deserialize(ObjectTree.builder()
                        .put(PluginServiceProvider.ClassTransformer.INST.getYamlEntryName(), List.of(
                            AccessTransformationImpl.class.getName(),
                            MixinTransformationImpl.class.getName()
                        )).build())
                ), new HorizonPlugin.NestedData(List.of(), List.of(), List.of())
            );
        } catch (Throwable thrown) {
            throw new RuntimeException("Couldn't build internal plugin", thrown);
        }

        try {
            start(providedArgs);
        } catch (Throwable thrown) {
            LOGGER.error(thrown, "Couldn't start Horizon server due to an unexpected exception!");
            System.exit(1);
        }
    }

    /**
     * The launch service for Horizon
     *
     * @return the Mixin launch service
     */
    public @NonNull MixinLaunch getLaunchService() {
        return MixinLaunch.getInstance();
    }

    /**
     * The optionset parsed for Horizon
     *
     * @return the Horizon optionset
     * @see ServerProperties
     */
    public @NonNull ServerProperties getProperties() {
        return this.properties;
    }

    /**
     * The Paperclip version info from the {@code version.json}
     *
     * @return the Paperclip {@code version.json}
     */
    public PaperclipVersion getVersionMeta() {
        return paperclipVersion;
    }

    /**
     * The server jar file of the Paperclip instance
     *
     * @return the server jar
     */
    public @NonNull FileJar getPaperclipJar() {
        return paperclipJar;
    }

    /**
     * Gets the JVM instrumentation
     *
     * @return the instrumentation
     * @see Instrumentation
     */
    public @NonNull Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * Gets the version of Horizon this is
     *
     * @return the version
     */
    public @NonNull String getVersion() {
        return version;
    }

    /**
     * Returns the {@link PluginTree} for the Horizon environment
     *
     * @return all plugins
     * @throws IllegalStateException if the server hasn't loaded Horizon plugins yet
     */
    public PluginTree getPlugins() {
        if (this.plugins == null) throw new IllegalStateException("Server hasn't loaded plugins yet");
        return this.plugins;
    }

    /**
     * Starts the Horizon server
     *
     * @param providedArgs the arguments provided to the server to be passed to the Minecraft main method
     */
    private void start(String[] providedArgs) {
        LOGGER.info("Preparing Minecraft server");
        this.plugins = EntrypointLoader.INSTANCE.init();

        final URL[] unpacked = prepareHorizonServer();

        for (URL url : unpacked) {
            try {
                Path asPath = Path.of(url.toURI());
                JvmAgent.addJar(asPath);
                initialClasspath.add(asPath);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException("Couldn't unpack and attach jar: " + url, e);
            }
        }

        for (HorizonPlugin plugin : this.plugins.getAll()) {
            // add all plugins to initial classpath
            initialClasspath.add(plugin.file().ioFile().toPath());

            // add all nested libraries like we are unpacking them as normal
            for (FileJar nestedLibrary : plugin.nestedData().libraryEntries()) {
                try {
                    LOGGER.info("Adding nested library jar '{}'", nestedLibrary.ioFile().getName());
                    Path asPath = Path.of(nestedLibrary.ioFile().toURI());
                    JvmAgent.addJar(asPath);
                    initialClasspath.add(asPath);
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't attach jar: " + nestedLibrary.jarFile().getName(), e);
                }
            }
        }

        try {
            initialClasspath.add(Path.of(HorizonLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

            MixinLaunch.launch(
                new MixinLaunch.LaunchContext(
                    providedArgs,
                    initialClasspath.stream()
                        .map(Path::toAbsolutePath)
                        .toList().toArray(new Path[0]),
                    Path.of(unpacked[0].toURI())
                )
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException("Couldn't locate self for setting up inital classpath", e);
        }
    }

    private URL @NonNull [] prepareHorizonServer() {
        final Path serverJarPath = getPaperclipJar().ioFile().toPath();
        boolean exists;
        try (final JarFile jarFile = new JarFile(serverJarPath.toFile())) {
            exists = jarFile.getJarEntry("version.json") != null;
        } catch (final IOException exception) {
            exists = false;
        }
        if (!exists) {
            LOGGER.error("Paperclip jar is invalid, couldn't locate version.json");
            System.exit(1);
        }

        try {
            final File file = serverJarPath.toFile();
            if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
            if (file.isDirectory() || !file.getName().endsWith(".jar"))
                throw new IOException("Provided path is not a jar file: " + file.toPath());

            try (final JarFile jarFile = new JarFile(file)) {
                // now we need to find where the server jar is located
                // build version info first
                ObjectTree versionTree = ObjectTree.read()
                    .format(Format.JSON)
                    .registerDeserializer(PaperclipVersion.class, new PaperclipVersion.PaperclipVersionDeserializer())
                    .registerDeserializer(PaperclipVersion.PackVersion.class, new PaperclipVersion.PackVersionDeserializer())
                    .from(new InputStreamReader(jarFile.getInputStream(jarFile.getJarEntry("version.json"))));

                this.paperclipVersion = versionTree.as(PaperclipVersion.class);

                try {
                    JvmAgent.addJar(HorizonLoader.INSTANCE.getPaperclipJar().ioFile().toPath());
                } catch (final IOException exception) {
                    throw new IllegalStateException("Unable to add paperclip jar to classpath!", exception);
                }

                // unpack libraries and patch
                return ServerPatcherEntrypoint.setupClasspath();
            }
        } catch (Throwable thrown) {
            throw new RuntimeException("Couldn't prepare server", thrown);
        }
    }
}
