package io.canvasmc.horizon.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class DummyClassLoader extends ClassLoader {
    private static final Enumeration<URL> DUMMY_ENUMERATION = new Enumeration<>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public @NonNull URL nextElement() {
            throw new NoSuchElementException();
        }
    };

    static {
        ClassLoader.registerAsParallelCapable();
    }

    @Override
    protected @NonNull Class<?> loadClass(final @NonNull String name, final boolean resolve) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    @Override
    public @Nullable URL getResource(final @NonNull String name) {
        return null;
    }

    @Override
    public @NonNull Enumeration<URL> getResources(final @NonNull String name) {
        return DummyClassLoader.DUMMY_ENUMERATION;
    }
}
