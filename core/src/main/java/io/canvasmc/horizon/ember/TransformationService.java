package io.canvasmc.horizon.ember;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * Represents a transformer service for Ember.
 *
 * @author vectrix
 * @since 1.0.0
 */
public interface TransformationService {
    /**
     * Executed after mixin has completed bootstrapping, but before the game has
     * launched.
     *
     * @since 1.0.0
     */
    void preboot();

    /**
     * Returns the priority of this transformer for the given {@link TransformPhase}.
     *
     * <p>A result of -1 means this transformer should not be applied during
     * the given phase.</p>
     *
     * <p>This method will be called multiple times for sorting the transformers
     * each class.</p>
     *
     * @param phase the transform phase
     * @return the priority
     * @since 1.0.0
     */
    int priority(final @NonNull TransformPhase phase);

    /**
     * Returns {@code true} if this transformer should transform the given
     * {@link Type} and {@link ClassNode}, otherwise returns {@code false}.
     *
     * @param type the type
     * @param node the class node
     * @return whether the class should be transformed
     * @since 1.0.0
     */
    boolean shouldTransform(final @NonNull Type type, final @NonNull ClassNode node);

    /**
     * Attempts to transform a class, with the given {@link Type}, {@link ClassNode}
     * and {@link TransformPhase} and returns the {@link ClassNode} if modifications were
     * made, otherwise returns {@code null}.
     *
     * @param type  the type
     * @param node  the class node
     * @param phase the transform phase
     * @return whether the class node if the class was transformed
     * @since 1.1.0
     */
    @Nullable ClassNode transform(final @NonNull Type type, final @NonNull ClassNode node, final @NonNull TransformPhase phase) throws Throwable;
}
