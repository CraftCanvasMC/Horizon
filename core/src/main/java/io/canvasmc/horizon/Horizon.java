package io.canvasmc.horizon;

import com.google.common.collect.ImmutableList;
import io.canvasmc.horizon.instrument.JvmAgent;
import io.canvasmc.horizon.instrument.patch.ServerPatcherEntrypoint;
import io.canvasmc.horizon.plugin.EntrypointLoader;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.MixinLaunch;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.PaperclipVersion;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Opcodes;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.jar.JarFile;

/**
 * The main class for Horizon that acts as a base
 * that runs the full startup and bootstrap process
 */
public class Horizon {
    public static final int ASM_VERSION = Opcodes.ASM9;
    public static final TaggedLogger LOGGER = Logger.tag("main");
    public static HorizonPlugin INTERNAL_PLUGIN;
    public static Horizon INSTANCE;

    private @NonNull
    final ServerProperties properties;
    private @NonNull
    final String version;
    private @NonNull
    final Instrumentation instrumentation;
    private @NonNull
    final FileJar paperclipJar;
    private List<HorizonPlugin> plugins;
    private PaperclipVersion paperclipVersion;

    public Horizon(@NonNull ServerProperties properties, @NonNull String version, @NonNull Instrumentation instrumentation, String @NonNull [] providedArgs) {
        this.properties = properties;
        this.version = version;
        this.instrumentation = instrumentation;

        INSTANCE = this;
        try {
            File paperclipIOFile = properties.serverJar();
            this.paperclipJar = new FileJar(paperclipIOFile, new JarFile(paperclipIOFile));

            INTERNAL_PLUGIN = new HorizonPlugin(
                "horizon",
                this.paperclipJar,
                new HorizonMetadata(
                    "horizon",
                    this.version,
                    "<void>",
                    "<void>",
                    List.of("internal.mixins.json"),
                    List.of(),
                    false
                ), new HorizonPlugin.NestedData(List.of(), List.of(), List.of())
            );
        } catch (IOException e) {
            throw new RuntimeException("Couldn't build FileJar", e);
        }

        try {
            start(providedArgs);
        } catch (Throwable thrown) {
            LOGGER.error(thrown, "Couldn't start Horizon server due to an unexpected exception!");
            System.exit(1);
        }
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
    public PaperclipVersion getPaperclipVersion() {
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
     * Get all the plugins in the Horizon server, including nested Horizon plugins
     *
     * @return all plugins
     * @apiNote if plugins aren't loaded yet, the list will be empty
     */
    public List<HorizonPlugin> getPlugins() {
        List<HorizonPlugin> allPlugins = new ArrayList<>();
        if (this.plugins == null) return List.of();

        Queue<HorizonPlugin> queue = new LinkedList<>(this.plugins);

        while (!queue.isEmpty()) {
            HorizonPlugin plugin = queue.poll();
            allPlugins.add(plugin);
            queue.addAll(plugin.nestedData().horizonEntries());
        }

        return allPlugins;
    }

    /**
     * Starts the Horizon server
     *
     * @param providedArgs the arguments provided to the server to be passed to the Minecraft main method
     */
    private void start(String[] providedArgs) {
        LOGGER.info("Preparing Minecraft server");
        this.plugins = ImmutableList.copyOf(EntrypointLoader.INSTANCE.init());

        final URL[] unpacked = prepareHorizonServer();
        final List<Path> initalClasspath = new ArrayList<>();

        for (URL url : unpacked) {
            try {
                Path asPath = Path.of(url.toURI());
                JvmAgent.addJar(asPath);
                initalClasspath.add(asPath);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException("Couldn't unpack and attach jar: " + url, e);
            }
        }

        for (HorizonPlugin plugin : getPlugins()) {
            // add all plugins to initial classpath
            initalClasspath.add(plugin.file().ioFile().toPath());

            // add all nested libraries like we are unpacking them as normal
            for (FileJar nestedLibrary : plugin.nestedData().libraryEntries()) {
                try {
                    LOGGER.info("Adding nested library jar '{}'", nestedLibrary.ioFile().getName());
                    Path asPath = Path.of(nestedLibrary.ioFile().toURI());
                    JvmAgent.addJar(asPath);
                    initalClasspath.add(asPath);
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't attach jar: " + nestedLibrary.jarFile().getName(), e);
                }
            }
        }

        try {
            initalClasspath.add(Path.of(Horizon.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

            MixinLaunch.launch(
                new MixinLaunch.LaunchContext(
                    providedArgs,
                    initalClasspath.stream()
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
                    JvmAgent.addJar(Horizon.INSTANCE.getPaperclipJar().ioFile().toPath());
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
