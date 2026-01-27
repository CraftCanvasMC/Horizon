package io.canvasmc.horizon.instrument.patch;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.canvasmc.horizon.HorizonLoader.LOGGER;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class ServerPatcherEntrypoint {

    private static final String META_INF = "/META-INF/";
    private static final String LIBRARIES_LIST = "libraries.list";
    private static final String VERSIONS_LIST = "versions.list";

    private static PatchEntry @NonNull [] findPatches() {
        try (InputStream stream = resource(META_INF + "patches.list")) {
            if (stream == null) return new PatchEntry[0];
            return PatchEntry.parse(reader(stream));
        } catch (IOException e) {
            throw Util.kill("Failed to read patches.list file", e);
        }
    }

    private static DownloadContext findDownloadContext() {
        final String line;
        try {
            line = Util.readResourceText(META_INF + "download-context");
        } catch (IOException e) {
            throw Util.kill("Failed to read download-context file", e);
        }
        return DownloadContext.parseLine(line);
    }

    private static @NonNull Path downloadOriginalJar(DownloadContext ctx, Path repoDir) {
        try {
            ctx.download(repoDir);
            return ctx.getOutputFile(repoDir);
        } catch (Throwable thrown) {
            throw Util.kill("Failed to download original jar", thrown);
        }
    }

    private static @NonNull Map<String, Map<String, URL>> extractAndApplyPatches(
        Path originalJar,
        PatchEntry[] patches,
        Path repoDir
    ) {
        if (originalJar == null && patches.length > 0) {
            throw new IllegalArgumentException("Patch data found without patch target");
        }

        final Map<String, Map<String, URL>> urls = new HashMap<>(2);

        try (FileSystem originalFs = (originalJar != null)
            ? FileSystems.newFileSystem(originalJar)
            : null) {

            final Path root = (originalFs != null) ? originalFs.getPath("/") : null;

            urls.put("versions", new HashMap<>());
            urls.put("libraries", new HashMap<>());

            extractEntries(urls.get("versions"), patches, root, repoDir,
                findEntries(VERSIONS_LIST), "versions");

            extractEntries(urls.get("libraries"), patches, root, repoDir,
                findEntries(LIBRARIES_LIST), "libraries");

        } catch (IOException e) {
            throw Util.kill("Failed to extract jar files", e);
        }

        if (patches.length > 0) {
            try (FileSystem fs = FileSystems.newFileSystem(originalJar)) {
                Path root = fs.getPath("/");
                for (PatchEntry p : patches) {
                    p.applyPatch(urls, root, repoDir);
                }
            } catch (IOException e) {
                throw Util.kill("Failed to apply patches", e);
            }
        }

        return urls;
    }

    private static InputStream resource(String name) {
        return ServerPatcherEntrypoint.class.getResourceAsStream(name);
    }

    private static @NonNull BufferedReader reader(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream));
    }

    private static void extractEntries(
        Map<String, URL> urls,
        PatchEntry[] patches,
        Path originalRoot,
        Path repoDir,
        Entry[] entries,
        String category
    ) throws IOException {
        if (entries == null) return;

        final String baseDir = META_INF.substring(1) + category;
        final Path outDir = repoDir.resolve(category);

        for (Entry e : entries) {
            if (!isPatchedFile(e, patches, category)) {
                extractFile(e, urls, originalRoot, outDir, baseDir);
            }
        }
    }

    private static Entry @Nullable [] findEntries(String file) {
        try (InputStream stream = resource(META_INF + file)) {
            return (stream != null) ? Entry.parse(reader(stream)) : null;
        } catch (IOException e) {
            throw Util.kill("Failed to read " + file + " file", e);
        }
    }

    private static boolean isPatchedFile(
        Entry entry,
        PatchEntry @NonNull [] patches,
        String category
    ) {
        for (PatchEntry p : patches) {
            if (p.location().equals(category) && p.outputPath().equals(entry.path())) {
                return true;
            }
        }
        return false;
    }

    private static void extractFile(
        @NonNull Entry entry,
        Map<String, URL> urls,
        Path originalRoot,
        @NonNull Path outDir,
        String baseDir
    ) throws IOException {

        final Path outputFile = outDir.resolve(entry.path());

        if (Files.exists(outputFile) && Util.isFileValid(outputFile, entry.hash())) {
            urls.put(entry.path(), outputFile.toUri().toURL());
            return;
        }

        String filePath = Util.endingSlash(baseDir) + entry.path();
        if (filePath.startsWith("/")) filePath = filePath.substring(1);
        InputStream in = readFromPatcherOrOriginal(filePath, originalRoot);

        Files.createDirectories(outputFile.getParent());
        Files.deleteIfExists(outputFile);

        try (in;
             ReadableByteChannel src = Channels.newChannel(in);
             FileChannel dst = FileChannel.open(outputFile, CREATE, WRITE, TRUNCATE_EXISTING)) {
            dst.transferFrom(src, 0, Long.MAX_VALUE);
        }

        if (!Util.isFileValid(outputFile, entry.hash())) {
            throw new IllegalStateException("Hash check failed for extracted file " + outputFile);
        }

        LOGGER.info("Unpacking Jar {} to {}", entry.id, outputFile);
        urls.put(entry.path(), outputFile.toUri().toURL());
    }

    private static @NonNull InputStream readFromPatcherOrOriginal(@NonNull String filePath, Path originalRoot)
        throws IOException {

        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        JarFile patcherJar = HorizonLoader.getInstance().getPaperclipJar().jarFile();

        JarEntry entry = patcherJar.getJarEntry(filePath);
        if (entry != null) {
            InputStream in = patcherJar.getInputStream(entry);
            if (in != null) {
                return in;
            }
        }

        if (originalRoot == null) {
            throw new IllegalStateException(filePath + " not found in patcher jar, no original provided");
        }

        Path orig = originalRoot.resolve(filePath);

        if (!Files.exists(orig)) {
            throw new IllegalStateException(filePath + " not found in patcher or original jar");
        }

        return Files.newInputStream(orig);
    }

    public static URL @NonNull [] setupClasspath() {
        final Path repoDir = Path.of(System.getProperty("bundlerRepoDir", ""));
        final PatchEntry[] patches = findPatches();
        final DownloadContext downloadContext = findDownloadContext();

        if (patches.length > 0 && downloadContext == null) {
            throw new IllegalArgumentException(
                "patches.list file found without a corresponding original-url file"
            );
        }

        final Path baseFile = (downloadContext != null)
            ? downloadOriginalJar(downloadContext, repoDir)
            : null;

        final Map<String, Map<String, URL>> classpathUrls =
            extractAndApplyPatches(baseFile, patches, repoDir);

        Collection<URL> versions = classpathUrls.getOrDefault("versions", Map.of()).values();
        Collection<URL> libraries = classpathUrls.getOrDefault("libraries", Map.of()).values();

        URL[] urls = new URL[versions.size() + libraries.size()];
        int i = 0;

        for (URL u : versions) urls[i++] = u;
        for (URL u : libraries) urls[i++] = u;

        return urls;
    }

    record Entry(byte[] hash, String id, String path) {

        private static @NonNull Entry parseLine(@NonNull String line) {
            String[] parts = line.split("\t");
            if (parts.length != 3) {
                throw new IllegalStateException("Malformed library entry: " + line);
            }
            return new Entry(Util.fromHex(parts[0]), parts[1], parts[2]);
        }

        static Entry @NonNull [] parse(@NonNull BufferedReader reader) throws IOException {
            List<Entry> list = new ArrayList<>(8);
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(parseLine(line));
            }
            return list.toArray(new Entry[0]);
        }
    }
}
