package io.canvasmc.horizon;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.canvasmc.horizon.instrument.JvmAgent;
import io.canvasmc.horizon.instrument.PaperclipTransformer;
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

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
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
    final File paperclipJar;
    private List<HorizonPlugin> plugins;
    private PaperclipVersion paperclipVersion;

    public Horizon(@NonNull OptionSet options, @NonNull String version, @NonNull Instrumentation instrumentation, String @NonNull [] providedArgs) {
        this.options = options;
        this.version = version;
        this.instrumentation = instrumentation;
        this.paperclipJar = (File) options.valueOf("serverjar");

        INSTANCE = this;
        try {
            INTERNAL_PLUGIN = new HorizonPlugin(
                "horizon",
                new FileJar(this.paperclipJar, new JarFile(this.paperclipJar)),
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

    private static void preparePaperclipLaunch() {
        // with paperclip, it contains System exits... this has to be removed
        JvmAgent.addTransformer(new PaperclipTransformer(PAPERCLIP_MAIN.replace('.', '/')));

        // we also launch the game ourselves, so we need this as patch only
        System.setProperty("paperclip.patchonly", "true");

        try {
            JvmAgent.addJar(Horizon.INSTANCE.getPaperclipJar().toPath());
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to add paperclip jar to classpath!", exception);
        }

        // launch paperclip for patching
        try {
            final Class<?> paperclipClass = Class.forName(PAPERCLIP_MAIN);
            paperclipClass
                .getMethod("main", String[].class)
                .invoke(null, (Object) new String[0]);
        } catch (ClassNotFoundException | NoSuchMethodException |
                 InvocationTargetException | IllegalAccessException thrown) {
            throw new IllegalStateException("Unable to execute paperclip jar!", thrown);
        }

        // cleanup the patchonly flag
        System.getProperties().remove("paperclip.patchonly");
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
    public @NonNull File getPaperclipJar() {
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

        final List<String> libraries = new ArrayList<>();
        final AtomicReference<String> game = new AtomicReference<>();

        prepareHorizonServer(game, libraries);

        final List<Path> initalClasspath = new ArrayList<>();
        final Path gameJar = Paths.get(game.get());

        for (HorizonPlugin plugin : plugins) {
            // add all plugins to initial classpath
            initalClasspath.add(plugin.file().ioFile().toPath());
        }

        try {
            JvmAgent.addJar(gameJar);
            initalClasspath.add(gameJar);

            Logger.trace("Added game jar: {}", gameJar);
        } catch (final IOException exception) {
            Logger.error(exception, "Failed to resolve game jar: {}", gameJar);
            System.exit(1);
            return;
        }

        libraries.forEach(path -> {
            if (!path.endsWith(".jar")) return;

            try {
                Path libPath = Paths.get(path);
                JvmAgent.addJar(libPath);
                initalClasspath.add(libPath);

                Logger.trace("Added game library jar: {}", path);
            } catch (final IOException exception) {
                Logger.error("Failed to resolve game library jar: {}", path);
                Logger.error(exception);
            }
        });

        try {
            initalClasspath.add(Path.of(Horizon.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Couldn't locate self for setting up inital classpath", e);
        }

        MixinLaunch.launch(
            new MixinLaunch.LaunchContext(
                providedArgs,
                initalClasspath.stream()
                    .map(Path::toAbsolutePath)
                    .toList().toArray(new Path[0]),
                gameJar
            )
        );
    }

    private void prepareHorizonServer(AtomicReference<String> game, List<String> libraries) {
        final Path serverJarPath = getPaperclipJar().toPath();
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
            // TODO - i dont want to invoke paperclip directly, this is stupid
            preparePaperclipLaunch();

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

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jarFile.getInputStream(jarFile.getJarEntry("META-INF/versions.list")))
                )) {
                    reader.lines()
                        .map(line -> line.split("\t", 3))
                        .filter(parts -> parts.length == 3)
                        .findFirst()
                        .ifPresent(parts -> {
                            String path = parts[2];
                            String[] split = path.split("/", 2);

                            String serverVer = split[1].split("-", 2)[0];

                            game.set("./versions/" + path);

                            Logger.info("Loading {} {} with Horizon version {}",
                                Character.toUpperCase(serverVer.charAt(0)) + serverVer.substring(1),
                                getPaperclipVersion().id(),
                                getVersion()
                            );
                        });
                }

                JarEntry entry = jarFile.getJarEntry("META-INF/libraries.list");
                if (entry != null) {
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(jarFile.getInputStream(entry)))
                    ) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\t", 3);
                            if (parts.length == 3) {
                                libraries.add("libraries/" + parts[2]);
                            }
                        }
                    }
                }
            }
        } catch (Throwable thrown) {
            throw new RuntimeException("Couldn't prepare server", thrown);
        }
    }
}
