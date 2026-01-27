package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.util.tree.ObjectArray;
import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

public class PluginServiceProvider {
    public static final Class<? extends ServiceType<String>> CLASS_TRANSFORMER = ClassTransformer.class;
    public static final Class<? extends ServiceType<Entrypoints.Entrypoint>> CUSTOM = Entrypoints.class;

    public static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "plugin_service_provider");
    public static final PluginServiceProvider EMPTY = new PluginServiceProvider(new Service[0]);

    public static final ObjectDeserializer<PluginServiceProvider> DESERIALIZER = new ObjectDeserializer<>() {
        @Contract(pure = true)
        @Override
        public @NonNull PluginServiceProvider deserialize(@NonNull ObjectTree tree) {
            List<Service<?>> services = new java.util.ArrayList<>();

            tree.getArrayOptional(ClassTransformer.INST.getYamlEntryName())
                .map(ClassTransformer.INST::parse)
                .ifPresent(parsed -> services.addAll(Arrays.asList(parsed)));

            tree.getArrayOptional(Entrypoints.INST.getYamlEntryName())
                .map(Entrypoints.INST::parse)
                .ifPresent(parsed -> services.addAll(Arrays.asList(parsed)));

            return new PluginServiceProvider(services.toArray(Service[]::new));
        }
    };

    private final Service[] services;

    private PluginServiceProvider(Service @NonNull [] services) {
        this.services = services;
        LOGGER.debug("Loaded services: {}", Arrays.toString(services));
    }

    public <E> Service<E>[] findServices(Class<? extends ServiceType<E>> clazz) {
        //noinspection unchecked
        return Arrays.stream(this.services)
            .filter(s -> clazz.isInstance(s.service()))
            .map(s -> (Service<E>) s)
            .toArray(Service[]::new);
    }

    public interface ServiceType<E> {

        String getYamlEntryName();

        Service<E>[] parse(ObjectArray array);
    }

    public static class ClassTransformer implements ServiceType<String> {

        public static final ClassTransformer INST = new ClassTransformer();

        @Override
        public String getYamlEntryName() {
            return "transform_service";
        }

        @Override
        public Service<String>[] parse(@NonNull ObjectArray array) {
            // we cannot load and init the class now, given at the time of parsing
            // we technically haven't added the plugin to the curr classloader yet
            List<String> entries = array.asList(String.class);
            Service<String>[] services = new Service[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                services[i] = new Service<>(this, entries.get(i));
            }
            return services;
        }
    }

    public static class Entrypoints implements ServiceType<Entrypoints.Entrypoint> {

        public static final Entrypoints INST = new Entrypoints();

        @Override
        public String getYamlEntryName() {
            return "entrypoint";
        }

        @Override
        public Service<Entrypoints.Entrypoint>[] parse(@NonNull ObjectArray array) {
            Service<Entrypoints.Entrypoint>[] services = new Service[array.size()];
            for (int i = 0; i < array.size(); i++) {
                ObjectTree raw = array.getTree(i);
                if (raw.isEmpty()) throw new IllegalArgumentException("Entrypoint entry cannot be empty");
                if (raw.keys().size() > 1)
                    throw new IllegalArgumentException("Entrypoint entry cannot contain more than 1 key/value pair");
                String entryId = raw.keys().stream().findFirst().orElseThrow();
                String target = raw.getValueOrThrow(entryId).asString();
                services[i] = new Service(this, new Entrypoints.Entrypoint(entryId, target));
            }
            return services;
        }

        public record Entrypoint(String entrypointer, String target) {

            @Override
            public @NonNull String toString() {
                return "Entrypoint{" +
                    "entrypointer='" + entrypointer + '\'' +
                    ", target='" + target + '\'' +
                    '}';
            }
        }
    }

    public record Service<E>(ServiceType<E> service, E obj) {

        @Override
        public @NonNull String toString() {
            return "service:" + service.getYamlEntryName() + "/" + obj.toString();
        }
    }
}
