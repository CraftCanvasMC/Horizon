package io.canvasmc.horizon.logger;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public enum Level {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    private final int severity;

    Level(int severity) {
        this.severity = severity;
    }

    @Contract(pure = true)
    public boolean isEnabled(@NonNull Level threshold) {
        return this.severity >= threshold.severity;
    }
}
