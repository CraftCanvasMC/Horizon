package io.canvasmc.horizon.transformer;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.ember.TransformPhase;
import io.canvasmc.horizon.ember.TransformationService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.ISyntheticClassRegistry;
import org.spongepowered.asm.transformers.MixinClassReader;

public final class MixinTransformationImpl implements TransformationService {
    private IMixinTransformerFactory transformerFactory;
    private IMixinTransformer transformer;
    private ISyntheticClassRegistry registry;

    public void offer(final @NonNull IMixinTransformerFactory factory) {
        this.transformerFactory = factory;
    }

    @Override
    public void preboot() {
        if (this.transformerFactory == null) throw new IllegalStateException("Transformer factory is not available!");
        this.transformer = this.transformerFactory.createTransformer();
        this.registry = this.transformer.getExtensions().getSyntheticClassRegistry();
    }

    @Override
    public int priority(final @NonNull TransformPhase phase) {
        if (phase == TransformPhase.MIXIN) return -1;
        return 50;
    }

    @Override
    public boolean shouldTransform(final @NonNull Type type, final @NonNull ClassNode node) {
        // transform everything
        return true;
    }

    @Override
    public @Nullable ClassNode transform(final @NonNull Type type, final @NonNull ClassNode node, final @NonNull TransformPhase phase) throws Throwable {
        if (this.shouldGenerateClass(type)) {
            return this.generateClass(type, node) ? node : null;
        }

        // transform via mixin
        return this.transformer.transformClass(MixinEnvironment.getCurrentEnvironment(), type.getClassName(), node) ? node : null;
    }

    public @NonNull ClassNode classNode(final @NonNull String canonicalName, final @NonNull String internalName, final byte @NonNull [] input, final int readerFlags) throws ClassNotFoundException {
        if (input.length != 0) {
            final ClassNode node = new ClassNode(Horizon.ASM_VERSION);
            final ClassReader reader = new MixinClassReader(input, canonicalName);
            reader.accept(node, readerFlags);
            return node;
        }

        final Type type = Type.getObjectType(internalName);
        if (this.shouldGenerateClass(type)) {
            final ClassNode node = new ClassNode(Horizon.ASM_VERSION);
            if (this.generateClass(type, node)) return node;
        }

        throw new ClassNotFoundException(canonicalName);
    }

    boolean shouldGenerateClass(final @NonNull Type type) {
        return this.registry.findSyntheticClass(type.getClassName()) != null;
    }

    boolean generateClass(final @NonNull Type type, final @NonNull ClassNode node) {
        return this.transformer.generateClass(MixinEnvironment.getCurrentEnvironment(), type.getClassName(), node);
    }
}
