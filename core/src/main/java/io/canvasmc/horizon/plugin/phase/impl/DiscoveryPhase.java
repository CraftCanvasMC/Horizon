package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.CandidateMetadata;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.data.HorizonMetadataDeserializer;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.Util;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static io.canvasmc.horizon.plugin.EntrypointLoader.LOGGER;

public class DiscoveryPhase implements Phase<Void, Set<PluginCandidate>> {

    @Override
    public Set<PluginCandidate> execute(Void input, @NonNull LoadContext context) throws PhaseException {
        Set<PluginCandidate> candidates = new HashSet<>();
        File pluginsDirectory = context.pluginsDirectory();
        File cacheDirectory = context.cacheDirectory();
        Util.clearDirectory(cacheDirectory); // cleanup directory

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            pluginsDirectory.toPath().toAbsolutePath(),
            path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))) {

            // also try and parse extra plugins
            Set<Path> files = Horizon.INSTANCE.getProperties().extraPlugins().stream().map(File::toPath).collect(Collectors.toSet());
            stream.forEach(files::add);

            for (Path path : files) {
                File child = path.toFile();
                LOGGER.debug("Scanning potential plugin: {}", child.getName());

                Optional<PluginCandidate> candidate = scanJarFile(child);
                candidate.ifPresent(candidates::add);
            }

        } catch (IOException e) {
            throw new PhaseException("Failed to scan plugins directory", e);
        }

        LOGGER.debug("Discovered {} plugin candidates", candidates.size());
        return candidates;
    }

    private Optional<PluginCandidate> scanJarFile(File jarFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            Optional<JarEntry> entry = jar.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().equalsIgnoreCase("paper-plugin.yml") ||
                    e.getName().equalsIgnoreCase("plugin.yml"))
                .findFirst();

            if (entry.isEmpty()) {
                LOGGER.debug("No plugin yaml found in {}", jarFile.getName());
                return Optional.empty();
            }

            try (InputStream in = jar.getInputStream(entry.get())) {
                ObjectTree yamlTree = ObjectTree.read()
                    // we also need to register all type converters
                    .registerDeserializer(HorizonMetadata.class, new HorizonMetadataDeserializer())
                    .format(Format.YAML).from(in);

                if (!yamlTree.containsKey("horizon")) {
                    LOGGER.debug("Not a Horizon plugin: {}", jarFile.getName());
                    return Optional.empty();
                }

                CandidateMetadata metadata = parseMetadata(yamlTree);
                // TODO - build plugin tree structure?

                LOGGER.debug("Searching for nested entries");

                List<FileJar> unpacked = new ArrayList<>();
                searchAndExtractNested(jar, unpacked);

                Set<PluginCandidate> nestedPluginCandidates = new HashSet<>();
                List<FileJar> nestedPlugins = new ArrayList<>();
                List<FileJar> nestedLibraries = new ArrayList<>();
                for (FileJar fileJar : unpacked) {
                    scanJarFile(fileJar.ioFile()).ifPresentOrElse(nestedPluginCandidates::add, () -> {
                        if (fileJar.jarFile().getJarEntry("paper-plugin.yml") != null ||
                            fileJar.jarFile().getJarEntry("plugin.yml") != null) {
                            // is plugin, not horizon though
                            LOGGER.debug("Found nested Paper plugin {}", fileJar.ioFile().getName());
                            nestedPlugins.add(fileJar);
                        } else nestedLibraries.add(fileJar);
                    });
                }

                LOGGER.debug("Found Horizon plugin: {} v{}", metadata.name(), metadata.version());
                return Optional.of(new PluginCandidate(new FileJar(jarFile, jar), metadata, new PluginCandidate.NestedData(
                    nestedPluginCandidates, nestedPlugins, nestedLibraries
                )));
            }
        } catch (Exception e) {
            LOGGER.error(e, "Error scanning jar file: {}", jarFile.getName());
            return Optional.empty();
        }
    }

    private void searchAndExtractNested(@NonNull JarFile jar, List<FileJar> nested) {
        jar.stream().filter(e -> e.getName().startsWith(Util.JIJ_PATH) && e.getName().endsWith(Util.JAR_SUFFIX)).forEachOrdered((entry) -> {
            // this is in META-INF/jars, so we need to extract and such
            // first, extract to cache dir
            File cacheDir = Horizon.INSTANCE.getProperties().cacheLocation();
            String fileName = entry.getName().substring(Util.JIJ_PATH.length());
            File extractedFile = new File(cacheDir, fileName);

            // defend against path traversal attempts
            try {
                String canonicalCachePath = cacheDir.getCanonicalPath();
                String canonicalFilePath = extractedFile.getCanonicalPath();
                if (!canonicalFilePath.startsWith(canonicalCachePath + File.separator)) {
                    LOGGER.error("Path traversal attempt detected: {}", entry.getName());
                    return;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to validate path for: {}", entry.getName(), e);
                return;
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                extractedFile.createNewFile();

                try (InputStream in = jar.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(extractedFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                LOGGER.debug("Loaded extracted entry {}", extractedFile.getName());
                nested.add(new FileJar(extractedFile, new JarFile(extractedFile)));
            } catch (IOException e) {
                throw new RuntimeException("Failed to extract nested JAR: " + entry.getName(), e);
            }
        });
    }

    private @NonNull CandidateMetadata parseMetadata(@NonNull ObjectTree data) {
        String name = data.getValueOptional("name").orElseThrow(() -> new IllegalArgumentException("Name must be present")).asString();
        String version = data.getValueOptional("version").orElseThrow(() -> new IllegalArgumentException("Name must be present")).asString();

        return new CandidateMetadata(name, version, data);
    }

    @Override
    public String getName() {
        return "Discovery";
    }
}