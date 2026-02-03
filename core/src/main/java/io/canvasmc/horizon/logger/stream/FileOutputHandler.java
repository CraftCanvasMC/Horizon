package io.canvasmc.horizon.logger.stream;

import io.canvasmc.horizon.logger.Level;
import io.canvasmc.horizon.logger.OutputHandler;
import org.jspecify.annotations.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Outputs logs to a specified {@link java.io.File}, allowing custom flush times too
 *
 * @author dueris
 */
public class FileOutputHandler implements OutputHandler {

    private final BufferedWriter writer;
    private final String filePath;

    FileOutputHandler(@NonNull File file, boolean append) throws IOException {
        this.filePath = file.getAbsolutePath();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        this.writer = new BufferedWriter(new FileWriter(file, append));
    }

    @Override
    public void write(String formattedMessage, Level level) {
        try {
            writer.write(formattedMessage);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + filePath);
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to flush log file: " + filePath);
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to close log file: " + filePath);
        }
    }
}
