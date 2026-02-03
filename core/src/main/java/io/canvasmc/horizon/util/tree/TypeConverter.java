package io.canvasmc.horizon.util.tree;

/**
 * Converter for transforming raw object values to specific types
 *
 * @param <T>
 *     the generic type for this converter
 *
 * @author dueris
 */
@FunctionalInterface
public interface TypeConverter<T> {
    T convert(Object value) throws Throwable;
}
