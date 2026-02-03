package io.canvasmc.horizon.logger;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
class LogEntry {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    final Level level;
    final String message;
    final String loggerName;
    final long timestamp;
    final String threadName;
    final String caller;
    final Throwable throwable;

    LogEntry(Level level, String message, String loggerName, Throwable throwable) {
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.throwable = throwable;
        this.timestamp = System.currentTimeMillis();
        this.threadName = Thread.currentThread().getName();
        this.caller = resolveCaller();
    }

    private String resolveCaller() {
        return STACK_WALKER.walk(stream ->
            stream
                .skip(1)
                .filter(frame -> {
                    String simpleName = frame.getDeclaringClass().getName();
                    return !simpleName.startsWith("io.canvasmc.horizon.logger");
                })
                .findFirst()
                .map(StackWalker.StackFrame::getDeclaringClass)
                .map(Class::getSimpleName)
                .orElse(null)
        );
    }
}
