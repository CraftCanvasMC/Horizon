package io.canvasmc.horizon.transformer;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.ember.TransformPhase;
import io.canvasmc.horizon.ember.TransformationService;
import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AccessTransformationImpl implements TransformationService {
    private final AccessWidener widener = new AccessWidener();
    private final AccessWidenerReader widenerReader = new AccessWidenerReader(this.widener);

    public void addWidener(final @NonNull Path path) throws IOException {
        try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            this.widenerReader.read(reader);
        }
    }

    @Override
    public void preboot() {
    }

    @Override
    public int priority(final @NonNull TransformPhase phase) {
        if (phase != TransformPhase.INITIALIZE) return -1;
        return 25;
    }

    @Override
    public boolean shouldTransform(final @NonNull Type type, final @NonNull ClassNode node) {
        return this.widener.getTargets().contains(node.name.replace('/', '.'));
    }

    @Override
    public @NonNull ClassNode transform(final @NonNull Type type, final @NonNull ClassNode node, final @NonNull TransformPhase phase) throws Throwable {
        final ClassNode writer = new ClassNode(Horizon.ASM_VERSION);
        final ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Horizon.ASM_VERSION, writer, this.widener);

        node.accept(visitor);

        return writer;
    }
}
