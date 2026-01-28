package io.canvasmc.horizon.util;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectArray;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.ParseError;
import io.canvasmc.horizon.util.tree.ParseException;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO - javadocs
public record ServerProperties(
    File pluginsDirectory,
    File serverJar,
    File cacheLocation,
    List<File> extraPlugins
) {
    private static final Pattern ADD_PLUGIN_PATTERN =
        Pattern.compile("^--?add-(plugin|extra-plugin-jar)=(.+)$");

    private static @NonNull List<File> extractExtraPlugins(@NonNull ObjectTree tree, String @NonNull [] args) {
        List<File> initial = new ArrayList<>();

        ObjectArray extraPluginsArray = tree.getArray("extraPlugins");
        for (int i = 0; i < extraPluginsArray.size(); i++) {
            String path = extraPluginsArray.get(i).asString();
            if (path == null) throw new IllegalArgumentException("Extra plugins array had a null entry at index " + i);
            initial.add(new File(path));
        }

        List<File> addedFromArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            Matcher matcher = ADD_PLUGIN_PATTERN.matcher(arg);
            if (matcher.matches()) {
                addedFromArgs.add(new File(matcher.group(2)));
                continue;
            }

            if (arg.equals("--add-plugin") || arg.equals("--add-extra-plugin-jar")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException(arg + " requires a path argument");
                }
                addedFromArgs.add(new File(args[++i]));
            }
        }

        initial.addAll(addedFromArgs);
        return initial;
    }

    public static @NonNull ServerProperties load(String[] args) {
        File file = new File("horizon.yml");
        try {
            ObjectTree defaultTree = ObjectTree.builder()
                .put("pluginsDirectory", "plugins")
                .put("serverJar", "server.jar")
                .put("cacheLocation", "cache/horizon")
                .put("extraPlugins", List.of())
                .build();

            // create default if not exist
            if (!file.exists()) {
                HorizonLoader.LOGGER.info("Configuration hasn't been loaded yet, building default and returning");
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();

                try (FileWriter writer = new FileWriter(file)) {
                    ObjectTree.write(defaultTree)
                        .format(Format.YAML)
                        .to(writer);
                }
            }
            else {
                HorizonLoader.LOGGER.debug("Configuration exists, loading...");
            }

            // read and parse configuration
            ObjectTree configTree = ObjectTree.read()
                .format(Format.YAML)
                // really should only be used for the runServer
                .registerOverrideKey("serverJar", "Horizon.serverJar")
                .registerOverrideKey("pluginsDirectory", "Horizon.pluginsDirectory")
                .registerOverrideKey("cacheLocation", "Horizon.cacheLocation")
                .registerDeserializer(ServerProperties.class, tree1 -> new ServerProperties(
                    tree1.getValueOrThrow("pluginsDirectory").as(File.class),
                    tree1.getValueOrThrow("serverJar").as(File.class),
                    tree1.getValueOrThrow("cacheLocation").as(File.class),
                    extractExtraPlugins(tree1, args)
                ))
                .from(new FileReader(file));

            // deserialize
            return configTree.as(ServerProperties.class);
        } catch (ParseException e) {
            HorizonLoader.LOGGER.error("Failed to parse configuration file");
            for (ParseError error : e.getErrors()) {
                HorizonLoader.LOGGER.error("  - {}", error);
            }
            throw Util.kill("Couldn't parse horizon properties, exiting", e);
        } catch (Throwable thrown) {
            throw Util.kill("Couldn't parse horizon properties, exiting", thrown);
        }
    }
}
