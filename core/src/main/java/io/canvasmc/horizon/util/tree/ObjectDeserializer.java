package io.canvasmc.horizon.util.tree;

/**
 * A deserializer for converting an {@link ObjectTree} to a custom {@link T} object
 */
@FunctionalInterface
public interface ObjectDeserializer<T> {

    T deserialize(ObjectTree tree) throws Exception;
}
