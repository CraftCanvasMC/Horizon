package io.canvasmc.horizon.logger.stream;

import io.canvasmc.horizon.logger.Level;
import io.canvasmc.horizon.logger.OutputHandler;

import java.io.OutputStream;
import java.io.PrintWriter;

public class StreamOutputHandler implements OutputHandler {

    private final PrintWriter writer;

    StreamOutputHandler(OutputStream stream, boolean autoFlush) {
        this.writer = new PrintWriter(stream, autoFlush);
    }

    @Override
    public void write(String formattedMessage, Level level) {
        writer.println(formattedMessage);
    }

    @Override
    public void flush() {
        writer.flush();
    }

    @Override
    public void close() {
        writer.close();
    }
}
