package io.canvasmc.horizon.service;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HorizonMixinLogger extends LoggerAdapterAbstract {

    private static final Map<String, ILogger> LOGGERS = new ConcurrentHashMap<>();

    private final Logger logger;

    HorizonMixinLogger(@NonNull String id) {
        super(id);
        this.logger = Logger.fork(HorizonLoader.LOGGER, id);
    }

    public static @NonNull ILogger get(@NonNull String name) {
        return LOGGERS.computeIfAbsent(name, HorizonMixinLogger::new);
    }

    @Override
    public String getType() {
        return "HorizonLogger";
    }

    @Override
    public void catching(@NonNull Level level, @NonNull Throwable throwable) {
        logThrowable(level, throwable);
    }

    @Override
    public void log(@NonNull Level level, @NonNull String message, @NonNull Object... args) {
        logMessage(level, message, args);
    }

    @Override
    public void log(@NonNull Level level, @NonNull String message, @NonNull Throwable throwable) {
        logWithThrowable(level, throwable, message);
    }

    @Override
    public <T extends Throwable> T throwing(@NonNull T throwable) {
        this.logger.error(throwable);
        return throwable;
    }

    private void logMessage(@NonNull Level level, String message, Object... args) {
        switch (level) {
            case TRACE -> this.logger.trace(message, args);
            case DEBUG -> this.logger.debug(message, args);
            case INFO -> this.logger.info(message, args);
            case WARN -> this.logger.warn(message, args);
            default -> this.logger.error(message, args);
        }
    }

    private void logThrowable(@NonNull Level level, Throwable throwable) {
        switch (level) {
            case TRACE -> this.logger.trace(throwable);
            case DEBUG -> this.logger.debug(throwable);
            case INFO -> this.logger.info(throwable);
            case WARN -> this.logger.warn(throwable);
            default -> this.logger.error(throwable);
        }
    }

    private void logWithThrowable(@NonNull Level level, Throwable throwable, String message) {
        switch (level) {
            case TRACE -> this.logger.trace(throwable, message);
            case DEBUG -> this.logger.debug(throwable, message);
            case INFO -> this.logger.info(throwable, message);
            case WARN -> this.logger.warn(throwable, message);
            default -> this.logger.error(throwable, message);
        }
    }
}
