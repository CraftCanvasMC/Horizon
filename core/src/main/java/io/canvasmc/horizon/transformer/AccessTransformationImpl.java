package io.canvasmc.horizon.transformer;

import io.canvasmc.horizon.service.transform.TransformPhase;
import io.canvasmc.horizon.service.transform.TransformationService;
import io.canvasmc.horizon.transformer.widener.TransformerContainer;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public final class AccessTransformationImpl implements TransformationService {

    private final TransformerContainer container = new TransformerContainer();
    private volatile boolean initialized = false;

    public TransformerContainer getContainer() {
        return container;
    }

    @Override
    public void preboot() {
        if (initialized) {
            return;
        }

        container.lock();
        initialized = true;
    }

    @Override
    public int priority(final @NonNull TransformPhase phase) {
        if (phase != TransformPhase.INITIALIZE) return -1;
        return 25;
    }

    @Override
    public boolean shouldTransform(final @NonNull Type type, final @NonNull ClassNode node) {
        if (!initialized) {
            return false;
        }

        return container.shouldTransform(node);
    }

    @Override
    public @NonNull ClassNode transform(final @NonNull Type type, final @NonNull ClassNode node, final @NonNull TransformPhase phase) throws Throwable {
        if (!initialized) {
            throw new IllegalStateException("Access transformation not initialized");
        }

        container.transformNode(node);
        return node;
    }
}
