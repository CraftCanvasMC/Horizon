package io.canvasmc.horizon.logger.stream;

import io.canvasmc.horizon.logger.Level;
import io.canvasmc.horizon.logger.OutputHandler;

import java.io.PrintStream;

public class ConsoleOutputHandler implements OutputHandler {

    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[91m";

    private final PrintStream stream;
    private final boolean allowColors;

    ConsoleOutputHandler(boolean allowColors) {
        this.allowColors = allowColors;
        this.stream = System.out;
    }

    @Override
    public void write(String formattedMessage, Level level) {
        if (!allowColors) {
            stream.println(formattedMessage);
            return;
        }

        String color = switch (level) {
            case WARN -> YELLOW;
            case ERROR -> RED;
            default -> null;
        };

        if (color == null) {
            stream.println(formattedMessage);
        }
        else {
            stream.println(color + formattedMessage + RESET);
        }
    }

    @Override
    public void flush() {
        stream.flush();
    }

    @Override
    public void close() {
    }
}
