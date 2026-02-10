package io.canvasmc.horizon.service;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.service.transform.ClassTransformer;
import io.canvasmc.horizon.service.transform.TransformPhase;
import io.canvasmc.horizon.util.DummyClassLoader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static io.canvasmc.horizon.HorizonLoader.LOGGER;
import static java.util.Objects.requireNonNull;

/**
 * Represents the transformation class loader.
 *
 * @author vectrix
 */
public final class EmberClassLoader extends ClassLoader {
    private static final List<String> EXCLUDE_PACKAGES = Arrays.asList(
        "java.", "javax.", "com.sun.", "org.objectweb.asm."
    );

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * The class transformer for this classloader
     */
    public final ClassTransformer transformer;

    private final Object lock = new Object();
    private final ClassLoader parent;
    private final DynamicClassLoader dynamic;
    private final Function<URLConnection, CodeSource> sourceLocator;
    private Function<URLConnection, Manifest> manifestLocator;
    private Predicate<String> transformationFilter;

    public EmberClassLoader(@NonNull List<Path> paths) {
        super("ember", new DynamicClassLoader(new URL[0]));

        this.parent = EmberClassLoader.class.getClassLoader();
        this.dynamic = (DynamicClassLoader) this.getParent();

        // add libraries, self, and game jar to classpath
        paths.forEach(this::tryAddToHorizonSystemLoader);

        // after loading all of that, create the transformer services
        this.transformer = new ClassTransformer();

        this.manifestLocator = connection -> this.locateManifest(connection).orElse(null);
        this.sourceLocator = connection -> this.locateSource(connection).orElse(null);
        this.transformationFilter = name -> EmberClassLoader.EXCLUDE_PACKAGES.stream().noneMatch(name::startsWith);
    }

    /**
     * Adds addition transformation paths and appends to the agent
     *
     * @param path
     *     a transformer path
     */
    public void tryAddToHorizonSystemLoader(Path path) {
        try {
            // Note: the instrumentation is designed in java to handle multiple
            //     invocations, so this is safe to be called multiple times
            HorizonLoader.getInstance().getInstrumentation().addJar(path);
            addTransformationPath(path);
        } catch (Throwable thrown) {
            throw new RuntimeException("Couldn't append to classpath", thrown);
        }
    }

    private @NonNull Optional<Manifest> locateManifest(final @NonNull URLConnection connection) {
        try {
            if (connection instanceof JarURLConnection) {
                return Optional.ofNullable(((JarURLConnection) connection).getManifest());
            }
        } catch (final IOException ignored) {
        }

        return Optional.empty();
    }

    private @NonNull Optional<CodeSource> locateSource(final @NonNull URLConnection connection) {
        if (connection instanceof JarURLConnection) {
            final URL url = ((JarURLConnection) connection).getJarFileURL();
            return Optional.of(new CodeSource(url, (Certificate[]) null));
        }

        return Optional.empty();
    }

    /**
     * Adds additional transformation paths.
     *
     * @param path
     *     a transformation path
     */
    public void addTransformationPath(final @NonNull Path path) {
        try {
            this.dynamic.addURL(path.toUri().toURL());
        } catch (final MalformedURLException exception) {
            LOGGER.error(exception, "Failed to resolve transformation path: {}", path);
        }
    }

    /**
     * Add the manifest locator.
     *
     * @param manifestLocator
     *     the manifest locator
     */
    public void addManifestLocator(final @NonNull Function<URLConnection, Optional<Manifest>> manifestLocator) {
        requireNonNull(manifestLocator, "manifestLocator");
        this.manifestLocator = this.alternate(manifestLocator, this::locateManifest);
    }

    private <I, O> @NonNull Function<I, O> alternate(final @Nullable Function<I, Optional<O>> first, final @Nullable Function<I, Optional<O>> second) {
        if (second == null && first != null) return input -> first.apply(input).orElse(null);
        if (first == null && second != null) return input -> second.apply(input).orElse(null);
        if (first != null) return input -> first.apply(input).orElseGet(() -> second.apply(input).orElse(null));
        return input -> null;
    }

    /**
     * Add the transformation filter.
     *
     * @param transformationFilter
     *     a transformation filter
     */
    public void addTransformationFilter(final @NonNull Predicate<String> transformationFilter) {
        requireNonNull(transformationFilter, "targetPackageFilter");
        this.transformationFilter = this.transformationFilter.and(transformationFilter);
    }

    boolean hasClass(final @NonNull String name) {
        final String canonicalName = name.replace('/', '.');
        return this.findLoadedClass(canonicalName) != null;
    }

    @Override
    protected @NonNull Class<?> loadClass(final @NonNull String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            final String canonicalName = name.replace('/', '.');

            Class<?> target = this.findLoadedClass(canonicalName);
            if (target == null) {
                if (canonicalName.startsWith("java.")) {
                    LOGGER.trace("Loading parent class: {}", canonicalName);
                    target = this.parent.loadClass(canonicalName);
                    LOGGER.trace("Loaded parent class: {}", canonicalName);
                }
                else {
                    LOGGER.trace("Attempting to load class: {}", canonicalName);
                    target = this.findClass(canonicalName, TransformPhase.INITIALIZE);
                    if (target == null) {
                        LOGGER.trace("Unable to locate class: {}", canonicalName);

                        LOGGER.trace("Attempting to load parent class: {}", canonicalName);
                        try {
                            target = this.parent.loadClass(canonicalName);
                            LOGGER.trace("Loaded parent class: {}", canonicalName);
                        } catch (final ClassNotFoundException exception) {
                            LOGGER.trace("Unable to locate parent class: {}", canonicalName);
                            throw exception;
                        }
                    }
                    else {
                        LOGGER.trace("Loaded transformed class: {}", canonicalName);
                    }
                }
            }

            if (resolve) this.resolveClass(target);
            return target;
        }
    }

    @Override
    protected @NonNull Class<?> findClass(final @NonNull String name) throws ClassNotFoundException {
        final String canonicalName = name.replace('/', '.');

        LOGGER.trace("Finding class: {}", canonicalName);
        final Class<?> target = this.findClass(canonicalName, TransformPhase.INITIALIZE);
        if (target == null) {
            LOGGER.trace("Unable to find class: {}", canonicalName);
            throw new ClassNotFoundException(canonicalName);
        }

        LOGGER.trace("Found class: {}", canonicalName);
        return target;
    }

    @Override
    public @Nullable URL getResource(final @NonNull String name) {
        requireNonNull(name, "name");

        URL url = this.dynamic.getResource(name);
        if (url == null) {
            url = this.parent.getResource(name);
        }

        return url;
    }

    @Override
    public @NonNull Enumeration<URL> getResources(final @NonNull String name) throws IOException {
        requireNonNull(name, "name");

        Enumeration<URL> resources = this.dynamic.getResources(name);
        if (!resources.hasMoreElements()) {
            resources = this.parent.getResources(name);
        }

        return resources;
    }

    @Override
    protected @Nullable URL findResource(final @NonNull String name) {
        requireNonNull(name, "name");
        return this.dynamic.findResource(name);
    }

    @Override
    protected @NonNull Enumeration<URL> findResources(final @NonNull String name) throws IOException {
        requireNonNull(name, "name");
        return this.dynamic.findResources(name);
    }

    @Override
    public @Nullable InputStream getResourceAsStream(final @NonNull String name) {
        requireNonNull(name, "name");

        InputStream stream = this.dynamic.getResourceAsStream(name);
        if (stream == null) {
            stream = this.parent.getResourceAsStream(name);
        }

        return stream;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable Class<?> findClass(final @NonNull String name, final @NonNull TransformPhase phase) {
        if (name.startsWith("java.")) {
            LOGGER.trace("Skipping platform class: {}", name);
            return null;
        }

        // Grab the class bytes.
        final ClassData transformed = this.transformData(name, phase);
        if (transformed == null) return null;

        // Check if the class has already been loaded by the transform.
        final Class<?> existingClass = this.findLoadedClass(name);
        if (existingClass != null) {
            LOGGER.trace("Skipping already defined transformed class: {}", name);
            return existingClass;
        }

        // Find the package for this class.
        final int classIndex = name.lastIndexOf('.');
        if (classIndex > 0) {
            final String packageName = name.substring(0, classIndex);
            this.findPackage(packageName, transformed.manifest());
        }

        final byte[] bytes = transformed.data();
        final ProtectionDomain domain = new ProtectionDomain(transformed.source(), null, this, null);
        return this.defineClass(name, bytes, 0, bytes.length, domain);
    }

    @Nullable ClassData transformData(final @NonNull String name, final @NonNull TransformPhase phase) {
        final ClassData data = this.classData(name, phase);
        if (data == null) return null;

        // Prevent transforming classes that are excluded from transformation.
        if (!this.transformationFilter.test(name)) {
            LOGGER.trace("Skipping transformer excluded class: {}", name);
            return null;
        }

        // Run the transformation.
        final byte[] bytes = this.transformer.transformBytes(name, data.data(), phase);
        return new ClassData(bytes, data.manifest(), data.source());
    }

    void findPackage(final @NonNull String name, final @Nullable Manifest manifest) {
        final Package target = this.getDefinedPackage(name);
        if (target == null) {
            synchronized (this.lock) {
                if (this.getDefinedPackage(name) != null) return;

                final String path = name.replace('.', '/').concat("/");
                String specTitle = null, specVersion = null, specVendor = null;
                String implTitle = null, implVersion = null, implVendor = null;

                if (manifest != null) {
                    final Attributes attributes = manifest.getAttributes(path);
                    if (attributes != null) {
                        specTitle = attributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
                        specVersion = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
                        specVendor = attributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                        implTitle = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                        implVersion = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                        implVendor = attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                    }

                    final Attributes mainAttributes = manifest.getMainAttributes();
                    if (mainAttributes != null) {
                        if (specTitle == null) specTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
                        if (specVersion == null)
                            specVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
                        if (specVendor == null)
                            specVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                        if (implTitle == null)
                            implTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                        if (implVersion == null)
                            implVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                        if (implVendor == null)
                            implVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                    }
                }

                this.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, null);
            }
        }
    }

    @Nullable ClassData classData(final @NonNull String name, final @NonNull TransformPhase phase) {
        final String resourceName = name.replace('.', '/').concat(".class");

        URL url = this.findResource(resourceName);
        if (url == null) {
            if (phase == TransformPhase.INITIALIZE) return null;
            url = this.parent.getResource(resourceName);
            if (url == null) return null;
        }

        return getClassData(url, resourceName);
    }

    /**
     * Gets the raw class data from the {@link java.net.URL} and resource name. Used internally for fetching the raw
     * file bytes of classes before transformation
     *
     * @param url
     *     the url in the classloader
     * @param resourceName
     *     the resource name
     *
     * @return the fetched class data, {@code null} if not found
     */
    public @Nullable ClassData getClassData(final URL url, final String resourceName) {
        try (final ResourceConnection connection = new ResourceConnection(url, this.manifestLocator, this.sourceLocator)) {
            final int length = connection.contentLength();
            final InputStream stream = connection.stream();
            final byte[] bytes = new byte[length];

            int position = 0, remain = length, read;
            while ((read = stream.read(bytes, position, remain)) != -1 && remain > 0) {
                position += read;
                remain -= read;
            }

            final Manifest manifest = connection.manifest();
            final CodeSource source = connection.source();
            return new ClassData(bytes, manifest, source);
        } catch (final Exception exception) {
            LOGGER.trace(exception, "Failed to resolve class data: {}", resourceName);
            return null;
        }
    }

    static final class DynamicClassLoader extends URLClassLoader {

        static {
            ClassLoader.registerAsParallelCapable();
        }

        DynamicClassLoader(final URL @NonNull [] urls) {
            super(urls, new DummyClassLoader());
        }

        @Override
        public void addURL(final @NonNull URL url) {
            super.addURL(url);
        }
    }

    /**
     * Represents the data for a class.
     *
     * @param data
     *     The class data as a byte array.
     * @param manifest
     *     The jar manifest
     * @param source
     *     The code source of the class
     *
     * @author dueris
     */
    public record ClassData(byte[] data, Manifest manifest, CodeSource source) {}

    private static final class ResourceConnection implements AutoCloseable {

        private final URLConnection connection;
        private final InputStream stream;
        private final Function<URLConnection, Manifest> manifestFunction;
        private final Function<URLConnection, CodeSource> sourceFunction;

        ResourceConnection(final @NonNull URL url,
                           final @NonNull Function<@NonNull URLConnection, @Nullable Manifest> manifestLocator,
                           final @NonNull Function<@NonNull URLConnection, @Nullable CodeSource> sourceLocator) throws IOException {
            this.connection = url.openConnection();
            this.stream = this.connection.getInputStream();
            this.manifestFunction = manifestLocator;
            this.sourceFunction = sourceLocator;
        }

        int contentLength() {
            return this.connection.getContentLength();
        }

        @NonNull InputStream stream() {
            return this.stream;
        }

        public @Nullable Manifest manifest() {
            return this.manifestFunction.apply(this.connection);
        }

        public @Nullable CodeSource source() {
            return this.sourceFunction.apply(this.connection);
        }

        @Override
        public void close() throws Exception {
            this.stream.close();
        }
    }
}
