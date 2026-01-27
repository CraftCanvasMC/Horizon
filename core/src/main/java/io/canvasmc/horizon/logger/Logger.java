package io.canvasmc.horizon.logger;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Contract(" -> new")
    public static @NonNull Builder create() {
        return new Builder();
    }

    public static Builder create(String name) {
        return new Builder().name(name);
    }

    public static Builder create(@NonNull Class<?> clazz) {
        return new Builder().name(clazz.getSimpleName());
    }

    @Contract("_, _ -> new")
    public static @NonNull Logger fork(@NonNull Logger parent, String name) {
        return new Logger(name, parent.minLevel, parent.formatter.pattern,
            parent.handlers.toArray(new OutputHandler[0]));
    }

    public void trace(String message, Object... args) {
        log(Level.TRACE, message, null, args);
    }

    private void log(@NonNull Level level, String message, Throwable throwable, Object[] args) {
        if (level.isEnabled(minLevel)) {
            LogEntry entry = new LogEntry(level, message, name, throwable);
            processor.submit(entry, formatter, handlers, args);
        }
    }

    public void trace(Throwable throwable, Object... args) {
        log(Level.TRACE, "", throwable, args);
    }

    public void trace(Throwable throwable, String message, Object... args) {
        log(Level.TRACE, message, throwable, args);
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, null, args);
    }

    public void debug(Throwable throwable, Object... args) {
        log(Level.DEBUG, "", throwable, args);
    }

    public void debug(Throwable throwable, String message, Object... args) {
        log(Level.DEBUG, message, throwable, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, null, args);
    }

    public void info(Throwable throwable, Object... args) {
        log(Level.INFO, "", throwable, args);
    }

    public void info(Throwable throwable, String message, Object... args) {
        log(Level.INFO, message, throwable, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARN, message, null, args);
    }

    public void warn(Throwable throwable, Object... args) {
        log(Level.WARN, "", throwable, args);
    }

    public void warn(Throwable throwable, String message, Object... args) {
        log(Level.WARN, message, throwable, args);
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, message, null, args);
    }

    public void error(Throwable throwable) {
        log(Level.ERROR, "", throwable, new Object[0]);
    }

    public void error(Throwable throwable, String message, Object... args) {
        log(Level.ERROR, message, throwable, args);
    }

    public boolean isTraceEnabled() {
        return Level.TRACE.isEnabled(minLevel);
    }

    public boolean isDebugEnabled() {
        return Level.DEBUG.isEnabled(minLevel);
    }

    public boolean isInfoEnabled() {
        return Level.INFO.isEnabled(minLevel);
    }

    public boolean isWarnEnabled() {
        return Level.WARN.isEnabled(minLevel);
    }

    public boolean isErrorEnabled() {
        return Level.ERROR.isEnabled(minLevel);
    }

    public String getName() {
        return name;
    }

    public void flush() {
        handlers.forEach(OutputHandler::flush);
    }

    public void close() {
        handlers.forEach(OutputHandler::close);
    }

    public static class Builder {

        private final List<OutputHandler> handlers = new ArrayList<>();
        private String name;
        private Level level = Level.INFO;
        private String pattern = null;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder out(OutputHandler... handlers) {
            this.handlers.addAll(Arrays.asList(handlers));
            return this;
        }

        public Logger build() {
            if (name == null) {
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                name = stack.length > 3 ? stack[3].getClassName() : "Unknown";
            }
            if (pattern == null) {
                throw new IllegalArgumentException("Pattern is required");
            }
            return new Logger(name, level, pattern, handlers.toArray(new OutputHandler[0]));
        }
    }
}
