package io.canvasmc.horizon;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import io.canvasmc.horizon.ember.EmberClassLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.transform.ClassTransformer;
import io.canvasmc.horizon.service.transform.TransformationService;
import io.canvasmc.horizon.util.ClassLoaders;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static io.canvasmc.horizon.HorizonLoader.LOGGER;

public final class MixinLaunch {
    public static final Pattern TRANSFORMATION_EXCLUDED_PATTERN = Pattern.compile(
        "^(?:" +
            "io\\.canvasmc\\.horizon\\.(?!inject\\.)" + "|" +
            "org\\.tinylog\\." + "|" +
            "org\\.spongepowered\\.asm\\." + "|" +
            "com\\.llamalad7\\.mixinextras\\." + "|" +
            "org\\.slf4j\\." + "|" +
            "org\\.apache\\.logging\\.log4j\\." +
            ").*"
    );
    public static final String[] TRANSFORMATION_EXCLUDED_RESOURCES = {
        "org/spongepowered/asm/"
    };

    private static final String JAVA_HOME = System.getProperty("java.home");
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<Manifest> DEFAULT_MANIFEST = Optional.of(new Manifest());
    private final ConcurrentMap<String, Optional<Manifest>> manifests = new ConcurrentHashMap<>();
    private final MixinLaunch.@NonNull LaunchContext context;
    private ClassTransformer transformer;
    private EmberClassLoader classLoader;

    MixinLaunch(@NonNull LaunchContext context) {
        this.context = context;
    }

    void run() {
        this.classLoader = new EmberClassLoader(Arrays.asList(this.context.initialGameConnections));
        this.transformer = this.classLoader.transformer;

        // set context classloader to ember classloader
        Thread.currentThread().setContextClassLoader(this.classLoader);

        for (final URL url : ClassLoaders.gatherSystemPaths()) {
            try {
                final URI uri = url.toURI();
                if (!this.transformable(uri)) {
                    LOGGER.debug("Skipped adding transformation path for: {}", url);
                    continue;
                }

                this.classLoader.addTransformationPath(Paths.get(url.toURI()));
                LOGGER.debug("Added transformation path for: {}", url);
            } catch (final URISyntaxException | IOException exception) {
                LOGGER.error(exception, "Failed to add transformation path for: {}", url);
            }
        }

        this.classLoader.addTransformationFilter(this.packageFilter());
        this.classLoader.addManifestLocator(this.manifestLocator());
        this.transformer.addExclusionFilter(this.resourceFilter());

        prepareMixin(HorizonLoader.getInstance().pluginLoader);

        try {
            if (Files.exists(this.context.gameJar)) {
                try (final JarFile file = new JarFile(this.context.gameJar.toFile())) {
                    String target = file.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                    LOGGER.info("Launching {}", target);
                    Thread runThread = new Thread(() -> {
                        try {
                            final Class<?> mainClass = Class.forName(target, true, classLoader);
                            final MethodHandle mainHandle = MethodHandles.lookup()
                                .findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class))
                                .asFixedArity();
                            mainHandle.invoke((Object) this.context.args);
                        } catch (Throwable thrown) {
                            LOGGER.error(thrown, "Unable to launch server");
                            System.exit(1);
                        }
                    }, "launch");
                    runThread.setContextClassLoader(this.classLoader);
                    runThread.start();
                }
            }
            else {
                throw new IllegalStateException("No game jar was found to launch!");
            }
        } catch (final Exception exception) {
            LOGGER.error(exception, "Failed to launch the game!");
        }
    }

    private void prepareMixin(@NonNull MixinPluginLoader pluginLoader) {
        MixinBootstrap.init();

        // finish plugin load, resolve mixin and wideners
        pluginLoader.finishPluginLoad(this.transformer);

        try {
            final Method method = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            method.setAccessible(true);
            method.invoke(null, MixinEnvironment.Phase.INIT);
            method.invoke(null, MixinEnvironment.Phase.DEFAULT);
        } catch (final Exception exception) {
            LOGGER.error(exception, "Failed to complete mixin bootstrap!");
        }

        for (final TransformationService transformer : this.transformer.services()) {
            transformer.preboot();
        }

        // init mixin extras
        MixinExtrasBootstrap.init();
    }

    public EmberClassLoader getClassLoader() {
        return classLoader;
    }

    public ClassTransformer getTransformer() {
        return transformer;
    }

    private @NonNull Predicate<String> packageFilter() {
        return name -> {
            return !name.matches(TRANSFORMATION_EXCLUDED_PATTERN.pattern());
        };
    }

    private @NonNull Predicate<String> resourceFilter() {
        return path -> {
            for (final String test : TRANSFORMATION_EXCLUDED_RESOURCES) {
                if (path.startsWith(test)) {
                    return false;
                }
            }

            return true;
        };
    }

    private @NonNull Function<URLConnection, Optional<Manifest>> manifestLocator() {
        return connection -> {
            if (connection instanceof JarURLConnection) {
                final URL url = ((JarURLConnection) connection).getJarFileURL();
                final Optional<Manifest> manifest = this.manifests.computeIfAbsent(url.toString(), key -> {
                    for (HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
                        try {
                            if (plugin.file().ioFile().toPath().toAbsolutePath().normalize().equals(Paths.get(url.toURI()).toAbsolutePath().normalize())) {
                                return Optional.ofNullable(plugin.file().jarFile().getManifest());
                            }
                        } catch (final URISyntaxException | IOException exception) {
                            LOGGER.error(exception, "Failed to load manifest from jar: {}", url);
                        }
                    }

                    return DEFAULT_MANIFEST;
                });

                try {
                    if (manifest == DEFAULT_MANIFEST) {
                        return Optional.ofNullable(((JarURLConnection) connection).getManifest());
                    }
                    else {
                        return manifest;
                    }
                } catch (final IOException exception) {
                    LOGGER.error(exception, "Failed to load manifest from connection for: {}", url);
                }
            }

            return Optional.empty();
        };
    }

    private boolean transformable(final @NonNull URI uri) throws URISyntaxException, IOException {
        final File target = new File(uri);

        if (target.getAbsolutePath().startsWith(JAVA_HOME)) {
            return false;
        }

        if (target.isDirectory()) {
            for (final String test : TRANSFORMATION_EXCLUDED_RESOURCES) {
                if (new File(target, test).exists()) {
                    return false;
                }
            }
        }
        else if (target.isFile()) {
            try (final JarFile jarFile = new JarFile(new File(uri))) {
                for (final String test : TRANSFORMATION_EXCLUDED_RESOURCES) {
                    if (jarFile.getEntry(test) != null) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public Path[] getInitialConnections() {
        return this.context.initialGameConnections;
    }

    public Path getGameJar() {
        return this.context.gameJar;
    }

    public String[] getLaunchArgs() {
        return this.context.args;
    }

    record LaunchContext(
        String[] args,
        Path[] initialGameConnections,
        Path gameJar
    ) {
    }
}
