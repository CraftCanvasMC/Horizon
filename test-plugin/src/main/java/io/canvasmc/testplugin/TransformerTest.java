package io.canvasmc.testplugin;

import io.canvasmc.horizon.service.transform.TransformPhase;
import io.canvasmc.horizon.service.transform.TransformationService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class TransformerTest implements TransformationService {
    @Override
    public void preboot() {
        System.out.println("A");
    }

    @Override
    public int priority(@NonNull TransformPhase phase) {
        return 0;
    }

    @Override
    public boolean shouldTransform(@NonNull Type type, @NonNull ClassNode node) {
        return false;
    }

    @Override
    public @Nullable ClassNode transform(@NonNull Type type, @NonNull ClassNode node, @NonNull TransformPhase phase) throws Throwable {
        return null;
    }
}
