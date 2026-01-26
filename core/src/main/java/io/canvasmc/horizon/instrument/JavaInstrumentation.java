package io.canvasmc.horizon.instrument;

import io.canvasmc.horizon.service.EmberClassLoader;
import org.jspecify.annotations.NonNull;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Path;
import java.util.jar.JarFile;

/**
 * An interface into the JVM Instrumentation for Horizon
 *
 * @author dueris
 * @see java.lang.instrument.Instrumentation
 */
public interface JavaInstrumentation {

    /**
     * Adds a jar to the system classloader. It is however recommended to use {@link EmberClassLoader#tryAddToHorizonSystemLoader(Path)}
     *
     * @param jar the jar file
     * @throws IllegalStateException if the instrumentation failed to boot or was unable to add
     */
    void addJar(final @NonNull JarFile jar);

    /**
     * Adds a jar to the system classloader. It is however recommended to use {@link EmberClassLoader#tryAddToHorizonSystemLoader(Path)}
     *
     * @param path the jar path
     * @throws IllegalStateException if the instrumentation failed to boot or was unable to add
     */
    void addJar(final @NonNull Path path);

    /**
     * Checks if the instrumentation booted successfully
     *
     * @throws IllegalStateException if the instrumentation failed to boot
     */
    void checkSuccess();

    /**
     * Adds a {@link ClassFileTransformer} to the system classloader
     *
     * @param transformer the transformer to be added
     * @see java.lang.instrument.Instrumentation#addTransformer(ClassFileTransformer, boolean)
     */
    void addTransformer(final @NonNull ClassFileTransformer transformer);
}
