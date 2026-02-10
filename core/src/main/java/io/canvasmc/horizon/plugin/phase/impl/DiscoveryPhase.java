package io.canvasmc.horizon.plugin.phase.impl;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.EntrypointObject;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.service.BootstrapMixinService;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.MinecraftVersion;
import io.canvasmc.horizon.util.Pair;
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

import static io.canvasmc.horizon.MixinPluginLoader.LOGGER;
import static io.canvasmc.horizon.plugin.data.HorizonPluginMetadata.ENTRYPOINT_CONVERTER;
import static io.canvasmc.horizon.plugin.data.HorizonPluginMetadata.PLUGIN_META_FACTORY;

public class DiscoveryPhase implements Phase<Void, Set<Pair<FileJar, HorizonPluginMetadata>>> {
    private static final String JIJ_PATH_HORIZON = "META-INF/jars/horizon/";
    private static final String JIJ_PATH_PAPER = "META-INF/jars/plugin/";
    private static final String JIJ_PATH_LIB = "META-INF/jars/libs/";

    private static void loadServerPlugin(final JarEntry jarEntry, final InputStream instream, final @NonNull FileJar pluginJar) throws Throwable {
        ObjectTree pluginYaml = ObjectTree.read().format(Format.YAML).from(instream);
        final String name = pluginYaml.getValueOrThrow("name").asString();
        // inject into setup classloader
        BootstrapMixinService.loadToInit(pluginJar.ioFile().toURI().toURL(), name);
    }

    @Override
    public Set<Pair<FileJar, HorizonPluginMetadata>> execute(Void input, @NonNull LoadContext context) throws PhaseException {
        Set<Pair<FileJar, HorizonPluginMetadata>> candidates = new HashSet<>();
        File pluginsDirectory = context.pluginsDirectory();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            pluginsDirectory.toPath().toAbsolutePath(),
            path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))) {

            // also try and parse extra plugins
            Set<Path> files = HorizonLoader.getInstance().getProperties().extraPlugins().stream().map(File::toPath).collect(Collectors.toSet());
            stream.forEach(files::add);

            for (Path path : files) {
                File child = path.toFile();
                LOGGER.debug("Scanning potential plugin: {}", child.getName());

                Optional<Pair<FileJar, HorizonPluginMetadata>> candidate = scanJarFile(child);
                candidate.ifPresent(candidates::add);
            }

        } catch (IOException e) {
            throw new PhaseException("Failed to scan plugins directory", e);
        }

        LOGGER.debug("Discovered {} plugin candidates", candidates.size());
        return candidates;
    }

    @Override
    public String getName() {
        return "Discovery";
    }

    private Optional<Pair<FileJar, HorizonPluginMetadata>> scanJarFile(File jarFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            Optional<JarEntry> entry = jar.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().equalsIgnoreCase("horizon.plugin.json"))
                .findFirst();

            if (entry.isEmpty()) {
                LOGGER.debug("No horizon json found in {}", jarFile.getName());
                // check if is spigot or paper plugin
                entry = jar.stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> e.getName().equalsIgnoreCase("paper-plugin.yml") ||
                        e.getName().equalsIgnoreCase("plugin.yml"))
                    .findFirst();
                // if is spigot or paper plugin, load into backup
                if (entry.isPresent()) {
                    try (InputStream in = jar.getInputStream(entry.get())) {
                        loadServerPlugin(entry.get(), in, new FileJar(jarFile, jar));
                    } catch (Throwable thrown) {
                        LOGGER.error(thrown, "Couldn't load server plugin {}", entry.get().getName());
                        return Optional.empty();
                    }
                }
                return Optional.empty();
            }

            try (InputStream in = jar.getInputStream(entry.get())) {
                ObjectTree jsonTree = ObjectTree.read()
                    // we also need to register all type converters
                    .registerConverter(EntrypointObject.class, ENTRYPOINT_CONVERTER)
                    .registerConverter(MinecraftVersion.class, value -> MinecraftVersion.fromStringId(value.toString()))
                    // now we need to register object deserializers
                    .registerDeserializer(HorizonPluginMetadata.class, PLUGIN_META_FACTORY)
                    // now we format and read
                    .format(Format.JSON).from(in);

                HorizonPluginMetadata metadata = jsonTree.as(HorizonPluginMetadata.class);
                HorizonPluginMetadata.NestedData nestedData = metadata.nesting();
                // Note: this loads recursively for nested horizon entries
                locateAndExtractJIJ(jar, (nested) -> {
                    switch (nested.type()) {
                        case PLUGIN -> {
                            FileJar pluginJar = nested.obj;
                            // we need to load this to the setup classloader so plugins can inject into
                            // jij server plugins, and also insert the plugin into storage
                            pluginJar.jarFile().stream()
                                .filter(e -> !e.isDirectory())
                                .filter(e -> e.getName().equalsIgnoreCase("paper-plugin.yml") ||
                                    e.getName().equalsIgnoreCase("plugin.yml"))
                                .findFirst().ifPresent((jarEntry) -> {
                                    try {
                                        try (InputStream instream = pluginJar.jarFile().getInputStream(jarEntry)) {
                                            loadServerPlugin(jarEntry, instream, pluginJar);
                                        }
                                    } catch (Throwable thrown) {
                                        throw new RuntimeException("Unable to load nested server plugin", thrown);
                                    }
                                });
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

                return Optional.of(new Pair<>(new FileJar(jarFile, jar), metadata));
            }
        } catch (Exception e) {
            LOGGER.error(e, "Error scanning jar file: {}", jarFile.getName());
            return Optional.empty();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void locateAndExtractJIJ(@NonNull JarFile jar, Consumer<NestedEntry> processor) {
        jar.stream()
            .filter(e -> e.getName().endsWith(Util.JAR_SUFFIX))
            .forEachOrdered(entry -> {

                final NestedEntry.Type type;
                final String n;

                if (entry.getName().startsWith(JIJ_PATH_HORIZON)) {
                    type = NestedEntry.Type.HORIZON;
                    n = JIJ_PATH_HORIZON;
                }
                else if (entry.getName().startsWith(JIJ_PATH_PAPER)) {
                    type = NestedEntry.Type.PLUGIN;
                    n = JIJ_PATH_PAPER;
                }
                else if (entry.getName().startsWith(JIJ_PATH_LIB)) {
                    type = NestedEntry.Type.LIBRARY;
                    n = JIJ_PATH_LIB;
                }
                else {
                    return;
                }

                File cacheDir = HorizonLoader.getInstance().getProperties().cacheLocation();
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
                } catch (IOException ioexe) {
                    LOGGER.error(ioexe, "Failed to validate path for: {}", entry.getName());
                    return;
                }

                try {
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

    private record NestedEntry(Type type, FileJar obj) {

        public enum Type {
            HORIZON,
            PLUGIN,
            LIBRARY
        }
    }
}