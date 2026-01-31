package io.canvasmc.horizon;

import io.canvasmc.horizon.instrument.JavaInstrumentation;
import io.canvasmc.horizon.instrument.JavaInstrumentationImpl;
import io.canvasmc.horizon.instrument.patch.ServerPatcherEntrypoint;
import io.canvasmc.horizon.logger.Level;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.logger.stream.OutStream;
import io.canvasmc.horizon.plugin.PluginTree;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.transformer.AccessTransformationImpl;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.PaperclipVersion;
import io.canvasmc.horizon.util.ServerProperties;
import io.canvasmc.horizon.util.Util;
import io.canvasmc.horizon.util.resolver.Artifact;
import io.canvasmc.horizon.util.resolver.DependencyResolver;
import io.canvasmc.horizon.util.resolver.Repository;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The main class for Horizon that acts as a base that runs the full startup and bootstrap process
 *
 * @author dueris
 */
public class HorizonLoader {
    public static final boolean DEBUG = Boolean.getBoolean("Horizon.debug");
    public static final Logger LOGGER = Logger.create()
        .name("main")
        .out(OutStream.CONSOLE.allowColors().build())
        .pattern("[{date: HH:mm:ss}] [{level}" + (DEBUG ? "/{tag}" : "") + "]: {message}")
        .level(DEBUG ? Level.DEBUG : Level.INFO)
        .build();
    public static final int JAVA_VERSION = Runtime.version().feature();

    static HorizonPlugin INTERNAL_PLUGIN;
    private static HorizonLoader INSTANCE;

    // final, all non-null
    protected @NonNull
    final MixinPluginLoader pluginLoader;
    private @NonNull
    final ServerProperties properties;
    private @NonNull
    final String version;
    private @NonNull
    final JavaInstrumentation instrumentation;
    private @NonNull
    final List<Path> initialClasspath;
    private @NonNull
    final FileJar paperclipJar;

    // nullable, gets created post-init
    private PluginTree plugins;
    private PaperclipVersion paperclipVersion;
    private MixinLaunch launchService;

    public HorizonLoader(@NonNull ServerProperties properties, @NonNull String version, @NonNull JavaInstrumentation instrumentation, @NonNull List<Path> initialClasspath, String @NonNull [] providedArgs) {
        this.properties = properties;
        this.version = version;
        this.instrumentation = instrumentation;
        this.initialClasspath = initialClasspath;
        this.pluginLoader = new MixinPluginLoader();

        INSTANCE = this;

        try {
            File paperclipIOFile = properties.serverJar();
            this.paperclipJar = new FileJar(paperclipIOFile, new JarFile(paperclipIOFile));

            File horizonIOFile = Path.of(HorizonLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();

            INTERNAL_PLUGIN = new HorizonPlugin(
                "horizon",
                new FileJar(horizonIOFile, new JarFile(horizonIOFile)),
                new HorizonPluginMetadata(
                    "Horizon",
                    "The core Horizon internals",
                    version,
                    new ArrayList<>(),
                    List.of(
                        AccessTransformationImpl.class.getName(),
                        MixinTransformationImpl.class.getName()
                    ),
                    List.of("CanvasMC"),
                    false,
                    List.of("internal.mixins.json"),
                    List.of("internal.at"),
                    ObjectTree.builder().build(),
                    new HorizonPluginMetadata.NestedData(Set.of(), Set.of(), Set.of())
                ), new HorizonPlugin.CompiledNestedPlugins(List.of(), List.of(), List.of())
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

    public static HorizonLoader getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Horizon loader hasn't been instantiated yet");
        }
        return INSTANCE;
    }

    public static void main(String[] args) {
        // TODO - can we support this..?
        if (Boolean.getBoolean("paper.useLegacyPluginLoading")) {
            throw new IllegalStateException("Legacy plugin loading is unsupported with Horizon");
        }
        String version;
        JarFile sourceJar;
        try {
            //noinspection resource
            sourceJar = new JarFile(HorizonLoader.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            Manifest manifest = sourceJar.getManifest();
            version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't fetch source jar", e);
        }

        List<Path> initialClasspath = new ArrayList<>();
        // init instrumentation interface early, we need this before we can access Horizon API
        JavaInstrumentation javaInstrumentation = new JavaInstrumentationImpl();

        // first, boot dependency resolver so we can actually run things without dying
        new DependencyResolver(new File("libraries"), () -> {
            return Util.parseFrom(sourceJar, "META-INF/artifacts.context", (line) -> {
                String[] split = line.split("\t");
                String id = split[0];
                String path = split[1];
                String sha256 = split[2];
                return new Artifact(id, path, sha256);
            }, Artifact.class);
        }, () -> {
            return Util.parseFrom(sourceJar, "META-INF/repositories.context", (line) -> {
                String[] split = line.split("\t");
                String name = split[0];
                URL url = URI.create(split[1]).toURL();
                return new Repository(name, url);
            }, Repository.class);
        }).resolve().forEach((jar) -> {
            initialClasspath.add(jar.ioFile().toPath());
            javaInstrumentation.addJar(jar.jarFile());
        });

        // load properties and start horizon init
        ServerProperties properties = ServerProperties.load(args);

        // cleanup directory for plugins
        File cacheDirectory = properties.cacheLocation();
        Util.clearDirectory(cacheDirectory);

        new HorizonLoader(properties, version, javaInstrumentation, initialClasspath, args);
    }

    /**
     * Starts the Horizon server
     *
     * @param providedArgs
     *     the arguments provided to the server to be passed to the Minecraft main method
     */
    private void start(String[] providedArgs) {
        final URL[] unpacked = prepareHorizonServer();
        this.plugins = this.pluginLoader.init();

        for (URL url : unpacked) {
            try {
                Path asPath = Path.of(url.toURI());
                getInstrumentation().addJar(asPath);
                initialClasspath.add(asPath);
            } catch (final Throwable thrown) {
                throw new RuntimeException("Couldn't unpack and attach jar: " + url, thrown);
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
                    getInstrumentation().addJar(asPath);
                    initialClasspath.add(asPath);
                } catch (final Throwable thrown) {
                    throw new RuntimeException("Couldn't attach jar: " + nestedLibrary.jarFile().getName(), thrown);
                }
            }
        }

        try {
            initialClasspath.add(Path.of(HorizonLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

            this.launchService = new MixinLaunch(
                new MixinLaunch.LaunchContext(
                    providedArgs,
                    initialClasspath.stream()
                        .map(Path::toAbsolutePath)
                        .toList().toArray(new Path[0]),
                    Path.of(unpacked[0].toURI())
                )
            );
            this.launchService.run();
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
                    .alias("resource_major", "resource")
                    .alias("data_major", "data")
                    .from(new InputStreamReader(jarFile.getInputStream(jarFile.getJarEntry("version.json"))));

                this.paperclipVersion = versionTree.as(PaperclipVersion.class);
                if (paperclipVersion.minecraftVersion().getJavaVersion() > JAVA_VERSION) {
                    throw new IllegalStateException("Minimum java version requirement isn't met for version " + paperclipVersion.minecraftVersion().getName() + ", please update your Java");
                }
                LOGGER.info("Booting Horizon {}", this.paperclipVersion.minecraftVersion().getName());

                try {
                    getInstrumentation().addJar(this.paperclipJar.jarFile());
                } catch (final Throwable thrown) {
                    throw new IllegalStateException("Unable to add paperclip jar to classpath!", thrown);
                }

                // unpack libraries and patch
                return ServerPatcherEntrypoint.setupClasspath();
            }
        } catch (Throwable thrown) {
            throw new RuntimeException("Couldn't prepare server", thrown);
        }
    }

    /**
     * Gets the JVM instrumentation
     *
     * @return the instrumentation
     *
     * @see Instrumentation
     * @see JavaInstrumentation
     */
    public @NonNull JavaInstrumentation getInstrumentation() {
        return instrumentation;
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
     * The launch service for Horizon
     *
     * @return the Mixin launch service
     */
    public @NonNull MixinLaunch getLaunchService() {
        if (this.launchService == null)
            throw new UnsupportedOperationException("Launch service hasn't been created yet");
        return this.launchService;
    }

    /**
     * The optionset parsed for Horizon
     *
     * @return the Horizon optionset
     *
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
     * Gets the version of Horizon this is
     *
     * @return the version
     */
    public @NonNull String getHorizonVersion() {
        return version;
    }

    /**
     * Returns the {@link PluginTree} for the Horizon environment
     *
     * @return all plugins
     *
     * @throws IllegalStateException
     *     if the server hasn't loaded Horizon plugins yet
     */
    public PluginTree getPlugins() {
        if (this.plugins == null) throw new IllegalStateException("Server hasn't loaded plugins yet");
        return this.plugins;
    }
}
