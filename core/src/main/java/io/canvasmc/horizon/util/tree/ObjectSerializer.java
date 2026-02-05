package io.canvasmc.horizon.util.tree;

/**
 * Serializer for converting custom {@link T} objects to an {@link ObjectTree}.
 *
 * @param <T>
 *     the generic type to deserialize
 *
 * @author dueris
 */
@FunctionalInterface
public interface ObjectSerializer<T> {
    ObjectTree serialize(T object) throws Exception;
}
