package io.canvasmc.horizon.util.tree;

public final class TypeConversionException extends RuntimeException {

    TypeConversionException(String message) {
        super(message);
    }

    TypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
