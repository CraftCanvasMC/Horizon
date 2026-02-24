package io.canvasmc.horizon.service.entrypoint;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.plugin.PluginTree;
import io.canvasmc.horizon.plugin.data.EntrypointObject;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class EntrypointContainer {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "entrypoint_api");

    /**
     * Constructs the entrypoint provider. This also runs most validation steps on the registered entrypoints configured
     * in the plugin metadata
     *
     * @param key
     *     the entrypoint key to invoke
     * @param interfaceClazz
     *     the interface all instances must implement
     * @param retType
     *     the return type, or {@link Void} if no return value
     * @param <C>
     *     the generic type for the interface
     * @param <R>
     *     the generic type for the return type
     *
     * @return the compiled entrypoint provider
     */
    @Contract("_, _, _ -> new")
    public static <C, R> @NonNull Provider<C, R> buildProvider(@NonNull String key, @NonNull Class<C> interfaceClazz, @NonNull Class<R> retType) {
        HorizonLoader loader = HorizonLoader.getInstance();
        PluginTree pluginTree = loader.getPlugins();

        if (!interfaceClazz.isInterface()) {
            throw new IllegalArgumentException("Class '" + interfaceClazz.getSimpleName() + "' is not an interface");
        }

        final List<EntryInstance<C>> finalizedInstances = new ArrayList<>();

        for (HorizonPlugin horizonPlugin : pluginTree.getAll()) {
            HorizonPluginMetadata metadata = horizonPlugin.pluginMetadata();

            for (final EntrypointObject entrypoint : metadata.entrypoints()) {
                if (!entrypoint.key().equalsIgnoreCase(key)) {
                    continue;
                }
                else {
                    // entry is of this key, try and find class and prepare
                    Class<? extends C> implementationClass;
                    String target = entrypoint.clazz();

                    try {
                        implementationClass = Class.forName(target, true, loader.getLaunchService().getClassLoader())
                            // verify implements interface
                            .asSubclass(interfaceClazz);

                        C built = implementationClass.getDeclaredConstructor()
                            .newInstance();

                        finalizedInstances.add(new EntryInstance<>(built, horizonPlugin, key, entrypoint.order()));
                    } catch (Throwable thrown) {
                        LOGGER.error(thrown, "Unable to find target class '" + target + "' for entrypoint '" + key + "'");
                        continue;
                    }
                }
                LOGGER.debug("Loaded provider {} for {}", entrypoint.clazz(), key);
            }
        }

        // return the built provider with all finalized instances
        return new Provider<>(
            interfaceClazz, retType, finalizedInstances
        );
    }

    /**
     * The entrypoint provider for a compiled key. This contains the methods and date to invoke the compiled entrypoints
     * from {@link io.canvasmc.horizon.service.entrypoint.EntrypointContainer#buildProvider(String, Class, Class)}
     *
     * @param <C>
     *     the generic type for the interface
     * @param <R>
     *     the generic type for the return type
     *
     * @author dueris
     */
    public static class Provider<C, R> {

        private final Class<C> requiredImplementation;
        private final Class<R> retType;

        private final List<EntryInstance<C>> instances;

        private BiConsumer<EntryInstance<C>, Throwable> errorHandler;

        private Provider(Class<C> requiredImplementation, Class<R> retType, @NonNull List<EntryInstance<C>> instances) {
            this.requiredImplementation = requiredImplementation;
            this.retType = retType;
            instances.sort(Comparator.comparingInt((EntryInstance<C> cEntryInstance) -> cEntryInstance.order));
            this.instances = Collections.unmodifiableList(instances);
        }

        /**
         * Invokes the entrypoint with specified arguments that will be passed to the methods being invoked in the
         * interface implementations
         *
         * @param args
         *     the arguments, can be empty
         *
         * @return a stream of return values. Empty stream if the return type is {@code void}
         */
        public Stream<R> invoke(Object... args) {
            if (!requiredImplementation.isAnnotationPresent(EntrypointHandler.class)) {
                throw new IllegalStateException("Entrypoint handler '" + requiredImplementation.getName() + "' must be annotated with @EntrypointHandler");
            }

            EntrypointHandler definition = requiredImplementation.getAnnotation(EntrypointHandler.class);

            // find method to invoke now
            Method toInvoke;

            try {
                toInvoke = requiredImplementation.getMethod(definition.value(), definition.argTypes());
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Handler definition points to invalid method", e);
            }

            if (!isVoid() && !retType.isAssignableFrom(toInvoke.getReturnType())) {
                throw new IllegalStateException("Method return type doesn't match definition");
            }

            Stream.Builder<R> stream = Stream.builder();

            for (EntryInstance<C> instance : instances) {
                try {
                    if (isVoid()) {
                        toInvoke.invoke(instance.built, args);
                        continue;
                    }
                    //noinspection unchecked
                    stream.add((R) toInvoke.invoke(instance.built, args));
                } catch (Throwable thrown) {
                    LOGGER.error("Failed to run entrypoint '{}' for plugin '{}'", instance.key, instance.horizonPlugin.pluginMetadata().name());
                    if (errorHandler != null) {
                        errorHandler.accept(instance, thrown);
                    }
                }
            }

            return stream.build();
        }

        /**
         * Get if the return type is {@code void}
         *
         * @return {@code true} if is {@code void}, {@code false} otherwise
         */
        private boolean isVoid() {
            return retType.equals(void.class) || retType.equals(Void.class);
        }

        /**
         * Get the return type of this entrypoint provider
         *
         * @return the return type
         */
        public Class<R> getReturnType() {
            return retType;
        }

        /**
         * Get the required interface implementation for this entrypoint provider
         *
         * @return the required interface class
         */
        public Class<C> getRequiredImplementation() {
            return requiredImplementation;
        }

        public void setErrorHandler(BiConsumer<EntryInstance<C>, Throwable> errorHandler) {
            this.errorHandler = errorHandler;
        }
    }

    public record EntryInstance<C>(C built, HorizonPlugin horizonPlugin, @NonNull String key, int order) {}
}
