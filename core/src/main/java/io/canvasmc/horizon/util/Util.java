package io.canvasmc.horizon.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinylog.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Util {
    public static MessageDigest SHA_256_DIGEST;

    static {
        try {
            SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException thrown) {
            throw kill("Couldn't create hashing inst", thrown);
        }
    }

    public static byte @NonNull [] readFromInStream(final @NonNull InputStream in) {
        try (in) {
            byte[] buf = new byte[1024 * 24];
            int off = 0;
            int read;
            while ((read = in.read(buf, off, buf.length - off)) != -1) {
                off += read;
                if (off == buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
            }
            return Arrays.copyOfRange(buf, 0, off);
        } catch (IOException e) {
            throw kill("Failed to read data from input stream", e);
        }
    }

    public static @Nullable String readResourceText(final @NonNull String path) throws IOException {
        final String p;
        if (path.startsWith("/")) {
            p = path;
        } else {
            p = "/" + path;
        }
        final InputStream stream = Util.class.getResourceAsStream(p);
        if (stream == null) {
            return null;
        }

        final StringWriter writer = new StringWriter();
        try (stream) {
            final Reader reader = new InputStreamReader(stream);
            reader.transferTo(writer);
        }

        return writer.toString();
    }

    public static boolean isDataValid(final byte[] data, final byte[] hash) {
        return Arrays.equals(hash, SHA_256_DIGEST.digest(data));
    }

    public static boolean isFileValid(final Path file, final byte[] hash) {
        if (Files.exists(file)) {
            final byte[] fileBytes;
            try {
                fileBytes = readFromInStream(Files.newInputStream(file));
            } catch (IOException e) {
                throw new RuntimeException("Couldn't read bytes", e);
            }
            return isDataValid(fileBytes, hash);
        }
        return false;
    }

    public static byte @NonNull [] fromHex(final @NonNull String s) {
        if (s.length() % 2 != 0) {
            throw new IllegalArgumentException("Length of hex " + s + " must be divisible by two");
        }
        try {
            final byte[] bytes = new byte[s.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                final char left = s.charAt(i * 2);
                final char right = s.charAt(i * 2 + 1);
                final byte b = (byte) ((getHexValue(left) << 4) | (getHexValue(right) & 0xF));
                bytes[i] = b;
            }
            return bytes;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot convert non-hex string: " + s);
        }
    }

    private static int getHexValue(final char c) {
        final int i = Character.digit(c, 16);
        if (i < 0) {
            throw new IllegalArgumentException("Invalid hex char: " + c);
        }
        return i;
    }

    public static @NonNull String endingSlash(final @NonNull String dir) {
        if (dir.endsWith("/")) {
            return dir;
        }
        return dir + "/";
    }

    public static @NonNull InternalError kill(final String message, final @Nullable Throwable thrown) {
        Logger.error(message);
        if (thrown != null) {
            Logger.error(thrown);
        }
        System.exit(1);
        return new InternalError();
    }
}
