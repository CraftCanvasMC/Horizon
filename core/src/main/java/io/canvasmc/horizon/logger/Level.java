package io.canvasmc.horizon.logger;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * Represents logging levels in ascending order of importance.
 * <p>
 * Each level has an associated integer value that determines its priority, with lower values representing more verbose
 * logging primarily used for debugging and higher values representing more critical messages, like warnings, errors,
 * and general logging levels
 *
 * @author dueris
 */
public enum Level {
    /**
     * The deepest logging level, primarily used for extreme debugging purposes
     */
    TRACE(0),
    /**
     * Detailed information commonly used for debugging purposes
     */
    DEBUG(1),
    /**
     * General information logging used during normal runtime
     */
    INFO(2),
    /**
     * Should be monitored, but not severe issues, primarily used to provide caution to application users
     */
    WARN(3),
    /**
     * Indicates actual errors, primarily used for when exceptions are thrown. Represents serious problems that should
     * probably be addressed
     */
    ERROR(4);

    private final int intLevel;

    Level(int intLevel) {
        this.intLevel = intLevel;
    }

    /**
     * Compares whether this level is enabled given the current threshold
     * <p>
     * A level is considered enabled if its priority is greater than or equal to the priority of the threshold
     *
     * @param threshold
     *     the threshold minimum logging level
     *
     * @return {@code true} if this level should be logged given the threshold, {@code false} otherwise
     */
    @Contract(pure = true)
    public boolean isEnabled(@NonNull Level threshold) {
        return this.intLevel >= threshold.intLevel;
    }
}
