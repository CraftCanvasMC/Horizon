package io.canvasmc.horizon.logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogProcessor {
    private static final Object lock = new Object();
    private static volatile LogProcessor instance;

    private final BlockingQueue<ProcessTask> queue;
    private final ExecutorService executor;
    private final AtomicBoolean running;

    private LogProcessor() {
        this.queue = new LinkedBlockingQueue<>(10000);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Horizon-Logger");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY + 1);
            return t;
        });
        this.running = new AtomicBoolean(true);

        executor.submit(this::processLogs);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static LogProcessor getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new LogProcessor();
                }
            }
        }
        return instance;
    }

    private void processLogs() {
        long lastFlushNanos = System.nanoTime();
        while (running.get() || !queue.isEmpty()) {
            try {
                ProcessTask task = queue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    String formatted = task.formatter.format(task.entry, task.args);
                    for (OutputHandler handler : task.handlers) {
                        try {
                            handler.write(formatted, task.entry.level);
                            // flush every 5ms or when we are in shutdown
                            if (lastFlushNanos + 5_000_000 <= System.nanoTime() || !running.get()) {
                                handler.flush();
                            }
                        } catch (Exception e) {
                            System.err.println("Error writing log: " + e.getMessage());
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        running.set(false);
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void submit(LogEntry entry, PatternFormatter formatter, List<OutputHandler> handlers, Object[] args) {
        if (!queue.offer(new ProcessTask(entry, formatter, handlers, args))) {
            System.err.println("Log queue full - dropping message: " + entry.message);
        }
    }

    private record ProcessTask(
        LogEntry entry, PatternFormatter formatter, List<OutputHandler> handlers,
        Object[] args) {}
}
