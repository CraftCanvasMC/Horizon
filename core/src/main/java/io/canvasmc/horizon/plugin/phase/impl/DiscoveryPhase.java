package io.canvasmc.horizon.plugin.phase.impl;

import com.google.common.collect.Sets;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static io.canvasmc.horizon.plugin.EntrypointLoader.LOGGER;

public class DiscoveryPhase implements Phase<Void, Set<PluginCandidate>> {
    public static final String JIJ_PATH_HORIZON = "META-INF/jars/horizon/";
    public static final String JIJ_PATH_PAPER = "META-INF/jars/plugin/";
    public static final String JIJ_PATH_LIB = "META-INF/jars/libs/";

    @Override
    public Set<PluginCandidate> execute(Void input, @NonNull LoadContext context) throws PhaseException {
        Set<PluginCandidate> candidates = new HashSet<>();
        File pluginsDirectory = context.pluginsDirectory();

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
                    return Optional.empty();
                }

                CandidateMetadata metadata = parseMetadata(yamlTree);
                // TODO - build plugin tree structure?

                PluginCandidate.NestedData nestedData = new PluginCandidate.NestedData(
                    Sets.newHashSet(),
                    Sets.newHashSet(),
                    Sets.newHashSet()
                );
                // Note: this loads recursively for nested horizon entries
                locateAndExtractJIJ(jar, (nested) -> {
                    switch (nested.type()) {
                        case PLUGIN -> {
                            nestedData.serverPluginEntries().add(nested.obj());
                            break;
                        }
                        case LIBRARY -> {
                            nestedData.libraryEntries().add(nested.obj());
                            break;
                        }
                        case HORIZON -> {
                            scanJarFile(nested.obj().ioFile()).ifPresent((candidate) -> {
                                // the IO file was a horizon jar
                                // Note: nested entries of child will be processed in the scanJarFile method
                                nestedData.horizonEntries().add(candidate);
                            });
                            break;
                        }
                    }
                });

                return Optional.of(new PluginCandidate(new FileJar(jarFile, jar), metadata, nestedData));
            }
        } catch (Exception e) {
            LOGGER.error(e, "Error scanning jar file: {}", jarFile.getName());
            return Optional.empty();
        }
    }

    private void locateAndExtractJIJ(@NonNull JarFile jar, Consumer<NestedEntry> processor) {
        jar.stream()
            .filter(e -> e.getName().endsWith(Util.JAR_SUFFIX))
            .forEachOrdered(entry -> {

                final NestedEntry.Type type;
                final String n;

                if (entry.getName().startsWith(JIJ_PATH_HORIZON)) {
                    type = NestedEntry.Type.HORIZON;
                    n = JIJ_PATH_HORIZON;
                } else if (entry.getName().startsWith(JIJ_PATH_PAPER)) {
                    type = NestedEntry.Type.PLUGIN;
                    n = JIJ_PATH_PAPER;
                } else if (entry.getName().startsWith(JIJ_PATH_LIB)) {
                    type = NestedEntry.Type.LIBRARY;
                    n = JIJ_PATH_LIB;
                } else {
                    return;
                }

                File cacheDir = Horizon.INSTANCE.getProperties().cacheLocation();
                String fileName = entry.getName().substring(n.length());
                File extractedFile = new File(cacheDir, fileName);

                // Path traversal defense
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
                    extractedFile.getParentFile().mkdirs();
                    extractedFile.createNewFile();

                    try (InputStream in = jar.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(extractedFile)) {

                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }

                    FileJar fileJar = new FileJar(extractedFile, new JarFile(extractedFile));
                    processor.accept(new NestedEntry(type, fileJar));

                    LOGGER.debug("Loaded extracted entry {}", extractedFile.getName());
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

    private record NestedEntry(Type type, FileJar obj) {
        public enum Type {
            HORIZON,
            PLUGIN,
            LIBRARY
        }
    }
}