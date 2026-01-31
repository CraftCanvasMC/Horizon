package io.canvasmc.horizon.util.tree;

/**
 * Converter for transforming mapped object values to specific types
 */
@FunctionalInterface
public interface MappedTypeConverter<E, S> {
    E convert(S value) throws Exception;
}
