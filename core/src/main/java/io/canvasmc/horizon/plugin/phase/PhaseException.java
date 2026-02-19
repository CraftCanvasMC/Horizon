package io.canvasmc.horizon.plugin.phase;

/**
 * Thrown when a phase couldn't complete
 *
 * @author dueris
 */
public class PhaseException extends Exception {

    public PhaseException(String message) {
        super(message);
    }

    public PhaseException(String message, Throwable cause) {
        super(message, cause);
    }
}