package io.canvasmc.horizon.service.transform;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * Defines a service capable of performing bytecode transformations
 * on classes during the game bootstrap lifecycle.
 *
 * <p>Implementations may participate in one or more
 * {@link TransformPhase transformation phases} and are invoked in
 * priority order within each phase.</p>
 */
public interface TransformationService {

    /**
     * Invoked after mixin has completed its bootstrap, but
     * before the game init begins.
     *
     * <p>This method may be used to perform one-time setup or
     * pre-initialization required by the transformer.</p>
     */
    void preboot();

    /**
     * Returns the execution priority of this transformer for the given
     * {@link TransformPhase}.
     *
     * <p>Lower values indicate earlier execution. A return value of
     * {@code -1} indicates that this transformer shouldn't be executed
     * in the specified phase.</p>
     *
     * <p>This method may be invoked multiple times while sorting
     * transformers for each transformed class.</p>
     *
     * @param phase the transformation phase
     * @return the priority for the given phase, or {@code -1} if inactive
     */
    int priority(final @NonNull TransformPhase phase);

    /**
     * Determines whether this transformer should be applied to the
     * specified class.
     *
     * @param type the ASM type representing the class
     * @param node the class node
     * @return {@code true} if the class should be transformed;
     * {@code false} otherwise
     */
    boolean shouldTransform(final @NonNull Type type, final @NonNull ClassNode node);

    /**
     * Attempts to transform the specified class during the given
     * {@link TransformPhase}.
     *
     * <p>If the transformer makes no modifications, this method must
     * return {@code null}. If modifications are made, the modified
     * {@link ClassNode} must be returned.</p>
     *
     * @param type  the ASM type representing the class
     * @param node  the class node to transform
     * @param phase the current transformation phase
     * @return the modified class node, or {@code null} if no changes were made
     * @throws Throwable if an unrecoverable error occurs during transformation
     */
    @Nullable ClassNode transform(
        final @NonNull Type type,
        final @NonNull ClassNode node,
        final @NonNull TransformPhase phase
    ) throws Throwable;
}
