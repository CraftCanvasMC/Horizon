package io.canvasmc.horizon.logger.stream;

import io.canvasmc.horizon.logger.OutputHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The holder for output stream configurations and builders
 *
 * @author dueris
 */
public class OutStream {

    /**
     * The console output stream. Logs entries to console, allowing colored logging, disabled by default.
     */
    public static ConsoleBuilder CONSOLE = new ConsoleBuilder();
    /**
     * The file output stream, logging entries to a file on a new line for each entry
     */
    public static FileBuilder FILE = new FileBuilder();
    /**
     * A provided output stream, and configurability for auto-flush
     */
    public static StreamBuilder STREAM = new StreamBuilder();

    public static class ConsoleBuilder {

        private boolean colored = false;

        public OutputHandler build() {
            return new ConsoleOutputHandler(colored);
        }

        public ConsoleBuilder allowColors() {
            this.colored = true;
            return this;
        }
    }

    public static class FileBuilder {

        private File file;
        private boolean append = true;

        public FileBuilder file(File file) {
            this.file = file;
            return this;
        }

        public FileBuilder append(boolean append) {
            this.append = append;
            return this;
        }

        public OutputHandler build() {
            if (file == null) {
                throw new IllegalStateException("File must be set");
            }
            try {
                return new FileOutputHandler(file, append);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file output handler", e);
            }
        }
    }

    public static class StreamBuilder {

        private OutputStream stream;
        private boolean autoFlush = true;

        public StreamBuilder stream(OutputStream stream) {
            this.stream = stream;
            return this;
        }

        public StreamBuilder autoFlush(boolean autoFlush) {
            this.autoFlush = autoFlush;
            return this;
        }

        public OutputHandler build() {
            if (stream == null) {
                throw new IllegalStateException("Stream must be set");
            }
            return new StreamOutputHandler(stream, autoFlush);
        }
    }
}
