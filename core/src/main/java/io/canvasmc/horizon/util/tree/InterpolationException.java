package io.canvasmc.horizon.util.tree;

/**
 * Thrown when interpolation fails during ObjectTree parsing
 *
 * @author dueris
 */
public final class InterpolationException extends RuntimeException {
    InterpolationException(String message) {
        super(message);
    }
}
