package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ParseError(String message, @Nullable Throwable cause) {
    public ParseError(String message) {
        this(message, null);
    }

    @Override
    public @NonNull String toString() {
        if (cause != null) {
            return message + " - " + cause.getMessage();
        }
        return message;
    }
}
