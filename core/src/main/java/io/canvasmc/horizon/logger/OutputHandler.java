package io.canvasmc.horizon.logger;

public interface OutputHandler {
    void write(String formattedMessage, Level level);

    void flush();

    void close();
}
