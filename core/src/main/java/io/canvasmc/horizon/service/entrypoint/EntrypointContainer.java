package io.canvasmc.horizon.service.entrypoint;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.plugin.PluginTree;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.data.PluginServiceProvider;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class EntrypointContainer {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "entrypoint_api");

    @Contract("_, _, _ -> new")
    public static <C, R> @NonNull Provider<C, R> buildProvider(@NonNull String key, @NonNull Class<C> interfaceClazz, @NonNull Class<R> retType) {
        HorizonLoader loader = HorizonLoader.getInstance();
        PluginTree pluginTree = loader.getPlugins();

        if (!interfaceClazz.isInterface()) {
            throw new IllegalArgumentException("Class '" + interfaceClazz.getSimpleName() + "' is not an interface");
        }

        final List<EntryInstance<C>> finalizedInstances = new ArrayList<>();

        for (HorizonPlugin horizonPlugin : pluginTree.getAll()) {
            HorizonMetadata metadata = horizonPlugin.pluginMetadata();
            PluginServiceProvider serviceProvider = metadata.serviceProvider();

            PluginServiceProvider.Service<PluginServiceProvider.Entrypoints.Entrypoint>[] services =
                serviceProvider.findServices(PluginServiceProvider.CUSTOM);

            // entrypointer == key
            // target == class to invoke
            for (PluginServiceProvider.Service<PluginServiceProvider.Entrypoints.Entrypoint> service : services) {
                PluginServiceProvider.Entrypoints.Entrypoint entry = service.obj();

                if (!entry.entrypointer().equalsIgnoreCase(key)) {
                    continue;
                }
                else {
                    // entry is of this key, try and find class and prepare
                    Class<? extends C> implementationClass;
                    String target = entry.target();

                    try {
                        implementationClass = Class.forName(target, true, loader.getLaunchService().getClassLoader())
                            // verify implements interface
                            .asSubclass(interfaceClazz);

                        C built = implementationClass.getDeclaredConstructor()
                            .newInstance();

                        finalizedInstances.add(new EntryInstance<>(built, horizonPlugin, key));
                    } catch (Throwable thrown) {
                        LOGGER.error(thrown, "Unable to find target class '" + target + "' for entrypoint '" + key + "'");
                        continue;
                    }
                }

                LOGGER.debug("Loaded provider {} for {}", entry.target(), key);
            }
        }

        // return the built provider with all finalized instances
        return new Provider<>(
            interfaceClazz, retType, Collections.unmodifiableList(finalizedInstances)
        );
    }

    public static class Provider<C, R> {

        private final Class<C> requiredImplementation;
        private final Class<R> retType;

        private final List<EntryInstance<C>> instances;

        private BiConsumer<EntryInstance<C>, Throwable> errorHandler;

        private Provider(Class<C> requiredImplementation, Class<R> retType, List<EntryInstance<C>> instances) {
            this.requiredImplementation = requiredImplementation;
            this.retType = retType;
            this.instances = instances;
        }

        // if ret type is void, run invoke void and return an empty stream
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
                    LOGGER.error("Failed to run entrypoint '{}' for plugin '{}'", instance.key, instance.horizonPlugin.identifier());
                    if (errorHandler != null) {
                        errorHandler.accept(instance, thrown);
                    }
                }
            }

            return stream.build();
        }

        private boolean isVoid() {
            return retType.equals(void.class) || retType.equals(Void.class);
        }

        public Class<R> getReturnType() {
            return retType;
        }

        public Class<C> getRequiredImplementation() {
            return requiredImplementation;
        }

        public void setErrorHandler(BiConsumer<EntryInstance<C>, Throwable> errorHandler) {
            this.errorHandler = errorHandler;
        }
    }

    public record EntryInstance<C>(C built, HorizonPlugin horizonPlugin, @NonNull String key) {
    }
}
