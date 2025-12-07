package io.canvasmc.horizon.instrument;

import org.jspecify.annotations.NonNull;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class JvmAgent {
    // Note: non-null guaranteed when accessed outside this class
    public static Instrumentation INSTRUMENTATION;

    public static void agentmain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        Logger.info("Booted from agent main");
        INSTRUMENTATION = inst;
    }

    public static void addTransformer(final @NonNull ClassFileTransformer transformer) {
        if (INSTRUMENTATION != null) INSTRUMENTATION.addTransformer(transformer);
    }

    public static void addJar(final @NonNull Path path) throws IOException {
        final File file = path.toFile();
        if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
        if (file.isDirectory() || !file.getName().endsWith(".jar"))
            throw new IOException("Provided path is not a jar file: " + path);
        addJar(new JarFile(file));
    }

    public static void addJar(final @NonNull JarFile jar) {
        if (INSTRUMENTATION != null) {
            Logger.debug("Appending jar '{}' to system classloader", jar.getName());
            INSTRUMENTATION.appendToSystemClassLoaderSearch(jar);
            return;
        }

        throw new IllegalStateException("Unable to addJar for '" + jar.getName() + "'.");
    }
}
