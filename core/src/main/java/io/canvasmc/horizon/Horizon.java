package io.canvasmc.horizon;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.canvasmc.horizon.instrument.JvmAgent;
import io.canvasmc.horizon.instrument.patch.ServerPatcherEntrypoint;
import io.canvasmc.horizon.plugin.EntrypointLoader;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.data.HorizonMetadataDeserializer;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.MixinLaunch;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.PaperclipVersion;
import joptsimple.OptionSet;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Opcodes;
import org.tinylog.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * The main class for Horizon that acts as a base
 * that runs the full startup and bootstrap process
 */
public class Horizon {
    public static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(HorizonMetadata.class, new HorizonMetadataDeserializer())
        .registerTypeAdapter(PaperclipVersion.class, new PaperclipVersion.PaperclipVersionDeserializer())
        .registerTypeAdapter(PaperclipVersion.class, new PaperclipVersion.PaperclipVersionSerializer())
        .create();
    public static final int ASM_VERSION = Opcodes.ASM9;
    private static final String PAPERCLIP_MAIN = "io.papermc.paperclip.Paperclip";
    public static HorizonPlugin INTERNAL_PLUGIN;

    public static Horizon INSTANCE;

    private @NonNull
    final OptionSet options;
    private @NonNull
    final String version;
    private @NonNull
    final Instrumentation instrumentation;
    private @NonNull
    final FileJar paperclipJar;
    private List<HorizonPlugin> plugins;
    private PaperclipVersion paperclipVersion;

    public Horizon(@NonNull OptionSet options, @NonNull String version, @NonNull Instrumentation instrumentation, String @NonNull [] providedArgs) {
        this.options = options;
        this.version = version;
        this.instrumentation = instrumentation;

        INSTANCE = this;
        try {
            File paperclipIOFile = (File) options.valueOf("serverjar");
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
                )
            );
        } catch (IOException e) {
            throw new RuntimeException("Couldn't build FileJar", e);
        }

        try {
            start(providedArgs);
        } catch (Throwable thrown) {
            Logger.error("Couldn't start Horizon server due to an unexpected exception!");
            Logger.error(thrown);
            System.exit(1);
        }
    }

    /**
     * The optionset parsed for Horizon
     *
     * @return the Horizon optionset
     * @see OptionSet
     */
    public @NonNull OptionSet getOptions() {
        return options;
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
     * Get all the plugins in the Horizon server
     * <p>
     * <b>Note:</b> if plugins aren't loaded yet, the list will be empty
     * </p>
     *
     * @return all plugins
     */
    public List<HorizonPlugin> getPlugins() {
        return this.plugins == null ? List.of() : this.plugins;
    }

    /**
     * Starts the Horizon server
     *
     * @param providedArgs the arguments provided to the server to be passed to the Minecraft main method
     */
    private void start(String[] providedArgs) {
        Logger.info("Preparing Minecraft server");
        this.plugins = ImmutableList.copyOf(EntrypointLoader.INSTANCE.init());

        final URL[] unpacked = prepareHorizonServer();
        final List<Path> initalClasspath = new ArrayList<>();

        for (HorizonPlugin plugin : plugins) {
            // add all plugins to initial classpath
            initalClasspath.add(plugin.file().ioFile().toPath());
        }

        for (URL url : unpacked) {
            try {
                Path asPath = Path.of(url.toURI());
                JvmAgent.addJar(asPath);
                initalClasspath.add(asPath);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException("Couldn't unpack and attach jar: " + url, e);
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
            Logger.error("Paperclip jar is invalid, couldn't locate version.json");
            System.exit(1);
        }

        try {
            final File file = serverJarPath.toFile();
            if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
            if (file.isDirectory() || !file.getName().endsWith(".jar"))
                throw new IOException("Provided path is not a jar file: " + file.toPath());

            try (final JarFile jarFile = new JarFile(file)) {
                // now we need to find where the server jar is located
                // build version info first=
                this.paperclipVersion = Horizon.GSON.fromJson(
                    new InputStreamReader(jarFile.getInputStream(jarFile.getJarEntry("version.json"))),
                    PaperclipVersion.class
                );

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
