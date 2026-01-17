package io.canvasmc.horizon.util;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import static io.canvasmc.horizon.HorizonLoader.LOGGER;

/**
 * Taken from <a href="https://github.com/cpw/grossjava9hacks/blob/1.3/src/main/java/cpw/mods/gross/Java9ClassLoaderUtil.java">grossjava9hacks</a>.
 *
 * @author cpw
 * @since 1.0.0
 */
public final class ClassLoaders {
    private ClassLoaders() {
    }

    /**
     * Returns the system class path {@link URL}s.
     *
     * @return the system class path urls
     */
    @SuppressWarnings({"restriction", "unchecked"})
    public static URL @NonNull [] gatherSystemPaths() {
        final ClassLoader classLoader = ClassLoaders.class.getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            try {
                final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                final Method objectFieldOffsetMethod = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
                final Method getObjectMethod = unsafeClass.getDeclaredMethod("getObject", Object.class, long.class);

                final Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                final Object unsafe = unsafeField.get(null);

                // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
                Field ucpField;
                try {
                    ucpField = classLoader.getClass().getDeclaredField("ucp");
                } catch (final NoSuchFieldException | SecurityException e) {
                    ucpField = classLoader.getClass().getSuperclass().getDeclaredField("ucp");
                }

                final long ucpFieldOffset = (long) objectFieldOffsetMethod.invoke(unsafe, ucpField);
                final Object ucpObject = getObjectMethod.invoke(unsafe, classLoader, ucpFieldOffset);

                // jdk.internal.loader.URLClassPath.path
                final Field pathField = ucpField.getType().getDeclaredField("path");
                final long pathFieldOffset = (long) objectFieldOffsetMethod.invoke(unsafe, pathField);
                final ArrayList<URL> path = (ArrayList<URL>) getObjectMethod.invoke(unsafe, ucpObject, pathFieldOffset);

                return path.toArray(new URL[0]);
            } catch (final Exception exception) {
                LOGGER.error(exception, "Failed to retrieve system classloader paths!");
                return new URL[0];
            }
        }

        return new URL[0];
    }
}
