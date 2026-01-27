package io.canvasmc.horizon.util.tree;

/**
 * Converter for transforming raw object values to specific types
 */
@FunctionalInterface
public interface TypeConverter<T> {

    T convert(Object value) throws Exception;
}
