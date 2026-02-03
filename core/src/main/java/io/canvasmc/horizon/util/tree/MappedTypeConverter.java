package io.canvasmc.horizon.util.tree;

/**
 * Converter for transforming mapped object values to specific types
 *
 * @param <E>
 *     the generic type representing the end result of the conversion
 * @param <S>
 *     the generic type representing the original type before the conversion
 *
 * @author dueris
 */
@FunctionalInterface
public interface MappedTypeConverter<E, S> {
    /**
     * Converts the provided generic to the end result generic type
     *
     * @param value
     *     the original value
     *
     * @return the converted value based off the original
     *
     * @throws Exception
     *     if conversion fails
     */
    E convert(S value) throws Throwable;
}
