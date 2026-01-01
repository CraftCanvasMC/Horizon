package io.canvasmc.horizon.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.canvasmc.horizon.Horizon.LOGGER;

public class Util {
    public static final String JAR_SUFFIX = ".jar";
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
        if (thrown != null) {
            LOGGER.error(thrown, message);
        } else LOGGER.error(message);
        System.exit(1);
        return new InternalError();
    }

    public static void clearDirectory(@NonNull File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                if (file.isDirectory()) {
                    clearDirectory(file);
                }
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            });
        }
    }

    private static boolean hasFileExtension(String path) {
        String name = new File(path).getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 && lastDot < name.length() - 1;
    }

    public static @NonNull File getOrCreateFile(String path) {
        File file = new File(path);
        boolean isDirectory = path.endsWith("/") || path.endsWith("\\") || !hasFileExtension(path);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs() && !parent.exists()) {
                    throw new IOException("Failed to create directories: " + parent);
                }
            }

            if (!file.exists()) {
                if (isDirectory) {
                    if (!file.mkdirs() && !file.exists()) {
                        throw new IOException("Failed to create directory: " + file);
                    }
                } else {
                    if (!file.createNewFile()) {
                        throw new IOException("Failed to create file: " + file);
                    }
                }
            }

            return file;
        } catch (IOException e) {
            throw new RuntimeException("Unable to get or create file: " + path, e);
        }
    }

    public static <E> E @NonNull [] parseFrom(@NonNull JarFile sourceJar, String entryName, ExceptionCatchingFunction<String, E> parser, Class<E> clazz) {
        List<E> elements = new ArrayList<>();
        JarEntry entry = sourceJar.getJarEntry(entryName);
        try (InputStream stream = sourceJar.getInputStream(entry)) {
            InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                elements.add(parser.transform(line));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Unable to read contents from \"" + sourceJar.getName() + ":" + entryName + "\"", e);
        }
        //noinspection unchecked
        return elements.toArray((E[]) Array.newInstance(clazz, 0));
    }
}
