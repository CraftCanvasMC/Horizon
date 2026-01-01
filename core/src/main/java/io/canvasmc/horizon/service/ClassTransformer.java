package io.canvasmc.horizon.service;

import com.google.common.collect.ImmutableList;
import io.canvasmc.horizon.ember.TransformPhase;
import io.canvasmc.horizon.ember.TransformationService;
import io.canvasmc.horizon.transformer.AccessTransformationImpl;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static io.canvasmc.horizon.Horizon.LOGGER;

public final class ClassTransformer {
    private static final List<TransformationService> SERVICES = ImmutableList.of(
        new AccessTransformationImpl(),
        new MixinTransformationImpl()
    );

    private final Map<Class<? extends TransformationService>, TransformationService> services;
    private final Map<TransformPhase, List<TransformationService>> orderedCache;

    private Predicate<String> exclusionFilter;

    public ClassTransformer() {
        this.orderedCache = new ConcurrentHashMap<>();
        this.services = new IdentityHashMap<>(SERVICES.size());
        this.exclusionFilter = path -> true;

        SERVICES.forEach(service -> services.put(service.getClass(), service));
        // TODO - plugin service registration
    }

    public void addExclusionFilter(final @NonNull Predicate<String> predicate) {
        this.exclusionFilter = predicate.and(predicate);
    }

    @SuppressWarnings("unchecked")
    public <T extends TransformationService> @Nullable T getService(final @NonNull Class<T> type) {
        return (T) services.get(type);
    }

    public @NonNull @UnmodifiableView Collection<TransformationService> services() {
        return Collections.unmodifiableCollection(this.services.values());
    }

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
        } else {
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
