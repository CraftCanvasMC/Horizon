package io.canvasmc.horizon.plugin.phase;

public class PhaseException extends Exception {

    public PhaseException(String message) {
        super(message);
    }

    public PhaseException(String message, Throwable cause) {
        super(message, cause);
    }
}