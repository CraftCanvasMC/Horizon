package io.canvasmc.horizon.instrument.patch;

import io.canvasmc.horizon.util.Util;
import org.jspecify.annotations.NonNull;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;

record DownloadContext(byte[] hash, URL url, String fileName) {
    private static final int BUFFER_SIZE = 32 * 1024; // 32 KiB

    public static DownloadContext parseLine(final String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        final String[] parts = line.split("\t", 3);
        if (parts.length != 3) {
            throw Util.kill("Invalid download-context line: " + line, null);
        }

        try {
            return new DownloadContext(
                Util.fromHex(parts[0]),
                URI.create(parts[1]).toURL(),
                parts[2]
            );
        } catch (MalformedURLException malformedURLException) {
            throw Util.kill("Invalid URL in download-context: " + parts[1], malformedURLException);
        }
    }

    public @NonNull Path getOutputFile(@NonNull Path outDir) {
        return outDir.resolve("cache").resolve(fileName);
    }

    public void download(Path outputDir) {
        final Path outputFile = getOutputFile(outputDir);

        if (Files.exists(outputFile) && Util.isFileValid(outputFile, hash)) {
            return;
        }

        try {
            Files.createDirectories(outputFile.getParent());
            Files.deleteIfExists(outputFile);

            Logger.info("Downloading {}", fileName);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.connect();

            final long contentLength = connection.getContentLengthLong();
            if (contentLength <= 0) {
                Logger.warn("Server did not provide content length for {}", fileName);
            }

            try (InputStream in = connection.getInputStream();
                 FileChannel fileChannel = FileChannel.open(outputFile, CREATE, WRITE, TRUNCATE_EXISTING)) {

                final byte[] buff = new byte[BUFFER_SIZE];
                long totalRead = 0;
                int n;

                long lastUpdate = System.currentTimeMillis();

                while ((n = in.read(buff)) != -1) {
                    //noinspection ResultOfMethodCallIgnored
                    fileChannel.write(ByteBuffer.wrap(buff, 0, n));
                    totalRead += n;

                    long now = System.currentTimeMillis();
                    if (now - lastUpdate >= 350) {
                        printProgress(totalRead, contentLength);
                        lastUpdate = now;
                    }
                }

                printProgress(contentLength, contentLength);
                System.out.println();
            }
        } catch (IOException e) {
            throw Util.kill("Couldn't download " + fileName, e);
        }

        if (!Util.isFileValid(outputFile, hash)) {
            throw Util.kill("Hash check failed for downloaded file " + fileName, null);
        }

        Logger.info("{} downloaded successfully.", fileName);
    }

    private void printProgress(long current, long total) {
        final int width = 50;
        double percent = (total > 0) ? (double) current / total : 0;
        int filled = (int) (percent * width);

        String bar = "\r" +
            '[' +
            "#".repeat(filled) +
            " ".repeat(Math.max(0, width - filled)) +
            "]" +
            String.format(" %6.2f%%", percent * 100);

        System.out.print(bar);
        System.out.flush();
    }
}
