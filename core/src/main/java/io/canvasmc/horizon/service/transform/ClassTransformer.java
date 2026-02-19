package io.canvasmc.horizon.service.transform;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static io.canvasmc.horizon.HorizonLoader.LOGGER;

/**
 * The class file transformer for Horizon. This manages and contains the
 * {@link io.canvasmc.horizon.service.transform.TransformationService} instances for class byte transformations, and
 * also conducts said transformations
 *
 * @author dueris
 */
public final class ClassTransformer {
    private final Map<Class<? extends TransformationService>, TransformationService> services;
    private final Map<TransformPhase, List<TransformationService>> orderedCache;

    private Predicate<String> exclusionFilter;

    public ClassTransformer() {
        this.orderedCache = new ConcurrentHashMap<>();
        this.services = new IdentityHashMap<>();
        this.exclusionFilter = path -> true;

        for (HorizonPlugin horizonPlugin : HorizonLoader.getInstance().getPlugins().getAll()) {
            for (String service : horizonPlugin.pluginMetadata().transformers()) {
                try {
                    Class<?> serviceClazz = Class.forName(service);
                    if (TransformationService.class.isAssignableFrom(serviceClazz)) {
                        TransformationService transformerObj = (TransformationService) serviceClazz.getDeclaredConstructor().newInstance();
                        services.put(transformerObj.getClass(), transformerObj);
                        LOGGER.debug("Registered class transformer from {}, \"{}\"", horizonPlugin.pluginMetadata().name(), serviceClazz.getName());
                    }
                    else
                        throw new IllegalArgumentException("Declared service class '" + service + "' is not instanceof a TransformationService");
                } catch (ClassNotFoundException exe) {
                    throw new IllegalArgumentException("The service '" + service + "' was not found or is invalid", exe);
                } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                         InstantiationException exe) {
                    throw new RuntimeException("Couldn't create transformer class '" + service + "'", exe);
                }
            }
        }
    }

    /**
     * Adds an exclusion filter to the package name filterer for bytecode transformation
     *
     * @param predicate
     *     the exclusion filter to add
     */
    public void addExclusionFilter(final @NonNull Predicate<String> predicate) {
        this.exclusionFilter = predicate.and(predicate);
    }

    /**
     * Gets the registered service based on the class of the service
     *
     * @param type
     *     the class of the service
     * @param <T>
     *     the generic type of the service
     *
     * @return the registered service instance
     */
    @SuppressWarnings("unchecked")
    public <T extends TransformationService> @Nullable T getService(final @NonNull Class<T> type) {
        return (T) services.get(type);
    }

    /**
     * Gets all the registered service instances in the class transformer
     *
     * @return all registered services
     */
    public @NonNull @UnmodifiableView Collection<TransformationService> getServices() {
        return Collections.unmodifiableCollection(this.services.values());
    }

    /**
     * Transforms the byte array input with the transformation services registered
     *
     * @param className
     *     the name of the class being transformed
     * @param input
     *     the byte array input
     * @param phase
     *     the current transformation phase
     *
     * @return the transformed byte array
     */
    public byte @NonNull [] transformBytes(final @NonNull String className, final byte @NonNull [] input, final @NonNull TransformPhase phase) {
        final String internalName = className.replace('.', '/');

        if (!this.exclusionFilter.test(internalName)) {
            LOGGER.debug("Skipping resource excluded class: {}", internalName);
            return input;
        }

        ClassNode node = new ClassNode(MixinTransformationImpl.ASM_VERSION);

        final Type type = Type.getObjectType(internalName);
        if (input.length > 0) {
            final ClassReader reader = new ClassReader(input);
            reader.accept(node, 0);
        }
        else {
            node.name = type.getInternalName();
            node.version = MixinEnvironment.getCompatibilityLevel().getClassVersion();
            node.superName = "java/lang/Object";
        }

        boolean transformed = false;
        for (final TransformationService service : this.getOrderedServices(phase)) {
            try {
                if (!service.shouldTransform(type, node)) continue;
                final ClassNode transformedNode = service.transform(type, node, phase);
                if (transformedNode != null) {
                    node = transformedNode;
                    transformed = true;
                }
            } catch (final Throwable throwable) {
                LOGGER.error(throwable, "Failed to transform {} with {}", type.getClassName(), service.getClass().getName());
            }
        }

        if (!transformed) return input;

        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);

        return writer.toByteArray();
    }

    private List<TransformationService> getOrderedServices(final @NonNull TransformPhase phase) {
        return orderedCache.computeIfAbsent(phase, p -> {
            final List<TransformationService> ordered = new ArrayList<>();

            services.values().stream()
                .filter(service -> service.priority(p) >= 0)
                .sorted(Comparator.comparingInt(service -> service.priority(p)))
                .forEach(ordered::add);

            return Collections.unmodifiableList(ordered);
        });
    }
}
