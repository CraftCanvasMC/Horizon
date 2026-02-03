package io.canvasmc.horizon.instrument;

import io.canvasmc.horizon.HorizonLoader;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

/**
 * The Java instrumentation for Horizon, and interface for Horizon plugins to use
 *
 * @author dueris
 * @see java.lang.instrument.Instrumentation
 */
public class JavaInstrumentationImpl implements JavaInstrumentation {

    // Note: non-null guaranteed when accessed outside this class
    private static Instrumentation INSTRUMENTATION;

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        HorizonLoader.LOGGER.debug("Booted from agent main");
        INSTRUMENTATION = inst;
    }

    @Override
    public void addTransformer(final @NonNull ClassFileTransformer transformer) {
        checkSuccess();

        INSTRUMENTATION.addTransformer(transformer);
    }

    @Override
    public void removeTransformer(final @NonNull ClassFileTransformer transformer) {
        checkSuccess();

        INSTRUMENTATION.removeTransformer(transformer);
    }

    @Override
    public void addJar(final @NonNull JarFile jar) {
        checkSuccess();

        HorizonLoader.LOGGER.debug("Appending jar '{}' to system classloader", jar.getName());
        INSTRUMENTATION.appendToSystemClassLoaderSearch(jar);
    }

    @Override
    public void addJar(final @NonNull Path path) {
        checkSuccess();

        final File file = path.toFile();
        try {
            if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
            if (file.isDirectory() || !file.getName().endsWith(".jar"))
                throw new IOException("Provided path is not a jar file: " + path);
            addJar(new JarFile(file));
        } catch (Throwable thrown) {
            throw new IllegalStateException("Unable to add jar to classpath", thrown);
        }
    }

    @Override
    public void checkSuccess() {
        if (INSTRUMENTATION == null) {
            throw new IllegalStateException("Instrumentation is null! Issue with agent booting?");
        }
    }
}
