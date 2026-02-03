package io.canvasmc.horizon.logger;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A flexible logging framework that has numerous features, like:
 *
 * <ul>
 *   <li>Logging levels</li>
 *   <li>Custom message formatting patterns</li>
 *   <li>Multiple output handlers for logging to different destinations</li>
 *   <li>Parameterized logging/object interpolation</li>
 *   <li>Exception logging with stack traces</li>
 *   <li>Asynchronous logging</li>
 *   <li>Logger "forking" for creating child loggers with inherited configuration</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Logger logger = Logger.create("MyApp")
 *     .level(Level.INFO)
 *     .pattern("[{date: HH:mm:ss}] [{level}] [{tag}]: {message}")
 *     .out(OutStream.CONSOLE.allowColors().build())
 *     .build();
 *
 * logger.info("Application started");
 * logger.debug("Processing request: {}", requestId);
 * logger.error(exception, "Failed to process request");
 * }</pre>
 *
 * <p>Thread Safety: This class is thread-safe. All log operations are processed asynchronously through a shared {@link LogProcessor}.</p>
 *
 * @author dueris
 * @see Level
 * @see OutputHandler
 * @see PatternFormatter
 * @see LogProcessor
 */
public class Logger {
    private final String name;
    private final Level minLevel;
    private final PatternFormatter formatter;
    private final List<OutputHandler> handlers;
    private final LogProcessor processor;

    private Logger(String name, Level minLevel, String pattern, OutputHandler... handlers) {
        this.name = name;
        this.minLevel = minLevel;
        this.formatter = new PatternFormatter(pattern);
        this.handlers = Arrays.asList(handlers);
        this.processor = LogProcessor.getInstance();
    }

    /**
     * Creates a {@link io.canvasmc.horizon.logger.Logger.Builder} for constructing a new logger
     *
     * @return a builder
     */
    @Contract(" -> new")
    public static @NonNull Builder create() {
        return new Builder();
    }

    /**
     * Creates a {@link io.canvasmc.horizon.logger.Logger.Builder} for constructing a new logger with a custom name
     *
     * @param name
     *     the name of the logger
     *
     * @return a named builder
     */
    public static Builder create(String name) {
        return new Builder().name(name);
    }

    /**
     * Creates a {@link io.canvasmc.horizon.logger.Logger.Builder} for constructing a new logger, with the name being
     * the simple name of the calling class
     *
     * @return a named builder, with the name being the simple name of the calling class
     */
    public static Builder createFromCaller() {
        Class<?> clazz = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .getCallerClass();
        return new Builder().name(clazz.getSimpleName());
    }

    /**
     * Forks the parent logger, creating a new child logger with a different name, but same configuration
     *
     * @param parent
     *     the parent logger
     * @param name
     *     the new name
     *
     * @return the forked logger
     */
    @Contract("_, _ -> new")
    public static @NonNull Logger fork(@NonNull Logger parent, String name) {
        return new Logger(name, parent.minLevel, parent.formatter.pattern,
            parent.handlers.toArray(new OutputHandler[0]));
    }

    private void log(@NonNull Level level, String message, Throwable throwable, Object[] args) {
        if (level.isEnabled(minLevel)) {
            LogEntry entry = new LogEntry(level, message, name, throwable);
            processor.submit(entry, formatter, handlers, args);
        }
    }

    /**
     * Logs a message at {@link io.canvasmc.horizon.logger.Level#TRACE}
     *
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void trace(String message, Object... args) {
        log(Level.TRACE, message, null, args);
    }

    /**
     * Logs a throwable at {@link io.canvasmc.horizon.logger.Level#TRACE}
     *
     * @param throwable
     *     the throwable
     */
    public void trace(Throwable throwable) {
        log(Level.TRACE, "", throwable, new Object[0]);
    }

    /**
     * Logs a throwable and message at {@link io.canvasmc.horizon.logger.Level#TRACE}
     *
     * @param throwable
     *     the throwable
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void trace(Throwable throwable, String message, Object... args) {
        log(Level.TRACE, message, throwable, args);
    }

    /**
     * Logs a message at {@link io.canvasmc.horizon.logger.Level#DEBUG}
     *
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, null, args);
    }

    /**
     * Logs a throwable at {@link io.canvasmc.horizon.logger.Level#DEBUG}
     *
     * @param throwable
     *     the throwable
     */
    public void debug(Throwable throwable) {
        log(Level.DEBUG, "", throwable, new Object[0]);
    }

    /**
     * Logs a throwable and message at {@link io.canvasmc.horizon.logger.Level#DEBUG}
     *
     * @param throwable
     *     the throwable
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void debug(Throwable throwable, String message, Object... args) {
        log(Level.DEBUG, message, throwable, args);
    }

    /**
     * Logs a message at {@link io.canvasmc.horizon.logger.Level#INFO}
     *
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void info(String message, Object... args) {
        log(Level.INFO, message, null, args);
    }

    /**
     * Logs a throwable at {@link io.canvasmc.horizon.logger.Level#INFO}
     *
     * @param throwable
     *     the throwable
     */
    public void info(Throwable throwable) {
        log(Level.INFO, "", throwable, new Object[0]);
    }

    /**
     * Logs a throwable and message at {@link io.canvasmc.horizon.logger.Level#INFO}
     *
     * @param throwable
     *     the throwable
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void info(Throwable throwable, String message, Object... args) {
        log(Level.INFO, message, throwable, args);
    }

    /**
     * Logs a message at {@link io.canvasmc.horizon.logger.Level#WARN}
     *
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void warn(String message, Object... args) {
        log(Level.WARN, message, null, args);
    }

    /**
     * Logs a throwable at {@link io.canvasmc.horizon.logger.Level#WARN}
     *
     * @param throwable
     *     the throwable
     */
    public void warn(Throwable throwable) {
        log(Level.WARN, "", throwable, new Object[0]);
    }

    /**
     * Logs a throwable and message at {@link io.canvasmc.horizon.logger.Level#WARN}
     *
     * @param throwable
     *     the throwable
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void warn(Throwable throwable, String message, Object... args) {
        log(Level.WARN, message, throwable, args);
    }

    /**
     * Logs a message at {@link io.canvasmc.horizon.logger.Level#ERROR}
     *
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void error(String message, Object... args) {
        log(Level.ERROR, message, null, args);
    }

    /**
     * Logs a throwable at {@link io.canvasmc.horizon.logger.Level#ERROR}
     *
     * @param throwable
     *     the throwable
     */
    public void error(Throwable throwable) {
        log(Level.ERROR, "", throwable, new Object[0]);
    }

    /**
     * Logs a throwable and message at {@link io.canvasmc.horizon.logger.Level#ERROR}
     *
     * @param throwable
     *     the throwable
     * @param message
     *     the message
     * @param args
     *     any object arguments to be interpolated
     */
    public void error(Throwable throwable, String message, Object... args) {
        log(Level.ERROR, message, throwable, args);
    }

    /**
     * Get if the {@code TRACE} logging level is enabled on this logger
     *
     * @return {@code true} if the level is enabled, {@code false} otherwise
     */
    public boolean isTraceEnabled() {
        return Level.TRACE.isEnabled(minLevel);
    }

    /**
     * Get if the {@code DEBUG} logging level is enabled on this logger
     *
     * @return {@code true} if the level is enabled, {@code false} otherwise
     */
    public boolean isDebugEnabled() {
        return Level.DEBUG.isEnabled(minLevel);
    }

    /**
     * Get if the {@code INFO} logging level is enabled on this logger
     *
     * @return {@code true} if the level is enabled, {@code false} otherwise
     */
    public boolean isInfoEnabled() {
        return Level.INFO.isEnabled(minLevel);
    }

    /**
     * Get if the {@code WARN} logging level is enabled on this logger
     *
     * @return {@code true} if the level is enabled, {@code false} otherwise
     */
    public boolean isWarnEnabled() {
        return Level.WARN.isEnabled(minLevel);
    }

    /**
     * Get if the {@code ERROR} logging level is enabled on this logger
     *
     * @return {@code true} if the level is enabled, {@code false} otherwise
     */
    public boolean isErrorEnabled() {
        return Level.ERROR.isEnabled(minLevel);
    }

    /**
     * Get the name of this logging instance
     *
     * @return the name of the logger
     */
    public String getName() {
        return name;
    }

    /**
     * Flushes all registered {@link io.canvasmc.horizon.logger.OutputHandler} registered to this logger via
     * {@link OutputHandler#flush()}
     */
    public void flush() {
        handlers.forEach(OutputHandler::flush);
    }

    /**
     * Closes all registered {@link io.canvasmc.horizon.logger.OutputHandler} registered to this logger via
     * {@link OutputHandler#close()}
     */
    public void close() {
        handlers.forEach(OutputHandler::close);
    }

    /**
     * A builder for constructing logger implementations
     *
     * @author dueris
     * @apiNote You must provide a pattern to construct the logger. The name, if not provided, will be the calling
     *     class simple name
     */
    public static class Builder {

        private final List<OutputHandler> handlers = new ArrayList<>();
        private String name;
        private Level level = Level.INFO;
        private String pattern = null;

        /**
         * Sets the name of the logger being built
         *
         * @param name
         *     the new name
         *
         * @return the builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the logging level for the logger being built
         *
         * @param level
         *     the logging level
         *
         * @return the builder
         */
        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        /**
         * Sets the pattern for the logger being built
         *
         * @param pattern
         *     the pattern
         *
         * @return the builder
         */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Adds output handler(s) to the logger being built
         *
         * @param handlers
         *     the handlers to be registered
         *
         * @return the builder
         */
        public Builder out(OutputHandler... handlers) {
            this.handlers.addAll(Arrays.asList(handlers));
            return this;
        }

        /**
         * Constructs the logger from the provided options via the builder
         *
         * @return the constructed logger
         */
        public Logger build() {
            if (name == null) {
                Class<?> clazz = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .getCallerClass();
                name = clazz.getSimpleName();
            }
            if (pattern == null) {
                throw new IllegalArgumentException("Pattern is required");
            }
            return new Logger(name, level, pattern, handlers.toArray(new OutputHandler[0]));
        }
    }
}
