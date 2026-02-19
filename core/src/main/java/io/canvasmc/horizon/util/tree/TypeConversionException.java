package io.canvasmc.horizon.util.tree;

/**
 * Thrown when unable to convert an {@link io.canvasmc.horizon.util.tree.ObjectValue} to a value type
 *
 * @author dueris
 */
public final class TypeConversionException extends RuntimeException {

    TypeConversionException(String message) {
        super(message);
    }

    TypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
