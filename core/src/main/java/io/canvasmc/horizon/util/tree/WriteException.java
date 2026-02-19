package io.canvasmc.horizon.util.tree;

/**
 * Thrown when unable to write an {@link io.canvasmc.horizon.util.tree.ObjectTree} to a specified output
 *
 * @author dueris
 */
public final class WriteException extends Exception {

    WriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
