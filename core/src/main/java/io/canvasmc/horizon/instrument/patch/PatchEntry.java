package io.canvasmc.horizon.instrument.patch;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.util.Util;
import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.Patch;
import org.apache.commons.compress.compressors.CompressorException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static io.canvasmc.horizon.Horizon.LOGGER;
import static java.nio.file.StandardOpenOption.*;

record PatchEntry(
    String location,
    byte[] originalHash,
    byte[] patchHash,
    byte[] outputHash,
    String originalPath,
    String patchPath,
    String outputPath
) {
    private static boolean bannerPrinted = false;

    public static PatchEntry @NonNull [] parse(@NonNull BufferedReader reader) throws IOException {
        List<PatchEntry> list = new ArrayList<>(4);

        String line;
        while ((line = reader.readLine()) != null) {
            PatchEntry p = parseLine(line);
            if (p != null) list.add(p);
        }

        return list.toArray(new PatchEntry[0]);
    }

    private static @Nullable PatchEntry parseLine(@NonNull String line) {
        if (line.isBlank() || line.startsWith("#")) return null;

        final String[] parts = line.split("\t");
        if (parts.length != 7) {
            throw new IllegalStateException("Invalid patch data line: " + line);
        }

        return new PatchEntry(
            parts[0],
            Util.fromHex(parts[1]),
            Util.fromHex(parts[2]),
            Util.fromHex(parts[3]),
            parts[4],
            parts[5],
            parts[6]
        );
    }

    public void applyPatch(
        Map<String, Map<String, URL>> urls,
        @NonNull Path originalRootDir,
        @NonNull Path repoDir
    ) throws IOException {
        Path inputDir = originalRootDir.resolve("META-INF").resolve(location);
        Path targetDir = repoDir.resolve(location);

        Path inputFile = inputDir.resolve(originalPath);
        Path outputFile = targetDir.resolve(outputPath);

        if (Files.exists(outputFile) && Util.isFileValid(outputFile, outputHash)) {
            urls.get(location).put(originalPath, outputFile.toUri().toURL());
            return;
        }

        announceOnce();

        validateInputFile(inputFile);
        byte[] originalBytes = Files.readAllBytes(inputFile);

        byte[] patchBytes = readPatchBytes();
        validatePatchBytes(patchBytes);

        Files.createDirectories(outputFile.getParent());

        try (OutputStream outStream =
                 new BufferedOutputStream(Files.newOutputStream(outputFile, CREATE, WRITE, TRUNCATE_EXISTING))) {

            Patch.patch(originalBytes, patchBytes, outStream);

        } catch (CompressorException | InvalidHeaderException | IOException e) {
            throw Util.kill("Failed to apply patch to " + inputFile, e);
        }

        if (!Util.isFileValid(outputFile, outputHash)) {
            throw new IllegalStateException("Output hash mismatch after patching " + outputPath);
        }

        urls.get(location).put(originalPath, outputFile.toUri().toURL());
    }

    private void announceOnce() {
        if (!bannerPrinted) {
            LOGGER.info("Applying patches...");
            bannerPrinted = true;
        }
    }

    private void validateInputFile(Path inputFile) {
        if (Files.notExists(inputFile)) {
            throw new IllegalStateException("Input file not found: " + inputFile);
        }
        if (!Util.isFileValid(inputFile, originalHash)) {
            throw new IllegalStateException("Original file hash mismatch: " + inputFile);
        }
    }

    private byte @NonNull [] readPatchBytes() throws IOException {
        String fullPatchPath = "META-INF/" + Util.endingSlash(location) + patchPath;
        JarFile jar = Horizon.INSTANCE.getPaperclipJar().jarFile();

        InputStream in = jar.getInputStream(jar.getEntry(fullPatchPath));
        if (in == null) {
            throw new IllegalStateException("Patch file not found in JAR: " + fullPatchPath);
        }

        return Util.readFromInStream(in);
    }

    private void validatePatchBytes(byte[] patchBytes) {
        if (!Util.isDataValid(patchBytes, patchHash)) {
            throw new IllegalStateException("Patch file hash mismatch for " + patchPath);
        }
    }
}
