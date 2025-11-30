package io.canvasmc.horizon.plugin.phase.impl;

import com.google.common.base.Preconditions;
import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.CandidateMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import io.canvasmc.horizon.util.FileJar;
import org.jspecify.annotations.NonNull;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class DiscoveryPhase implements Phase<Void, Set<PluginCandidate>> {

    @Override
    public Set<PluginCandidate> execute(Void input, @NonNull LoadContext context) throws PhaseException {
        Set<PluginCandidate> candidates = new HashSet<>();
        File pluginsDirectory = context.pluginsDirectory();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            pluginsDirectory.toPath().toAbsolutePath(),
            path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))) {

            // also try and parse extra plugins
            //noinspection unchecked
            Set<Path> files = ((List<File>) Horizon.INSTANCE.getOptions().valuesOf("add-plugin")).stream().map(File::toPath).collect(Collectors.toSet());
            stream.forEach(files::add);

            for (Path path : files) {
                File child = path.toFile();
                Logger.debug("Scanning potential plugin: {}", child.getName());

                Optional<PluginCandidate> candidate = scanJarFile(child);
                candidate.ifPresent(candidates::add);
            }

        } catch (IOException e) {
            throw new PhaseException("Failed to scan plugins directory", e);
        }

        Logger.debug("Discovered {} plugin candidates", candidates.size());
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
                Logger.debug("No plugin yaml found in {}", jarFile.getName());
                return Optional.empty();
            }

            try (InputStream in = jar.getInputStream(entry.get())) {
                Map<String, Object> data = Horizon.YAML.load(in);

                if (!data.containsKey("horizon")) {
                    Logger.debug("Not a Horizon plugin: {}", jarFile.getName());
                    return Optional.empty();
                }

                CandidateMetadata metadata = parseMetadata(data);

                Logger.debug("Found Horizon plugin: {} v{}", metadata.name(), metadata.version());
                return Optional.of(new PluginCandidate(new FileJar(jarFile, jar), metadata));
            }
        } catch (Exception e) {
            Logger.error(e, "Error scanning jar file: {}", jarFile.getName());
            return Optional.empty();
        }
    }

    private @NonNull CandidateMetadata parseMetadata(@NonNull Map<String, Object> data) {
        String name = Preconditions.checkNotNull(data.get("name"), "Name must be present").toString();
        String version = Preconditions.checkNotNull(data.get("version"), "Version must be present").toString();

        return new CandidateMetadata(name, version, data);
    }

    @Override
    public String getName() {
        return "Discovery";
    }
}