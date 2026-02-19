package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an error when an {@link io.canvasmc.horizon.util.tree.ObjectTree} fails for any reason
 *
 * @param message
 *     the description of the parse error
 * @param cause
 *     the <b>nullable</b> throwable
 *
 * @author dueris
 */
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
