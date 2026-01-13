package io.canvasmc.horizon;

import io.canvasmc.horizon.util.Util;
import io.canvasmc.horizon.util.tree.*;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

// TODO - javadocs
public record ServerProperties(
    File pluginsDirectory,
    File serverJar,
    File cacheLocation,
    List<File> extraPlugins
) {

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
                Horizon.LOGGER.info("Configuration hasn't been loaded yet, building default and returning");
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();

                try (FileWriter writer = new FileWriter(file)) {
                    ObjectTree.write(defaultTree)
                        .format(Format.YAML)
                        .to(writer);
                }
            } else {
                Horizon.LOGGER.info("Configuration exists, loading...");
            }

            // read and parse configuration
            ObjectTree configTree = ObjectTree.read()
                .format(Format.YAML)
                .registerDeserializer(ServerProperties.class, tree1 -> new ServerProperties(
                    tree1.getValue("pluginsDirectory").as(File.class),
                    tree1.getValue("serverJar").as(File.class),
                    tree1.getValue("cacheLocation").as(File.class),
                    extractExtraPlugins(tree1, args)
                ))
                .from(new FileReader(file));

            // deserialize
            return configTree.as(ServerProperties.class);
        } catch (ParseException e) {
            Horizon.LOGGER.error("Failed to parse configuration file");
            for (ParseError error : e.getErrors()) {
                Horizon.LOGGER.error("  - {}", error);
            }
            throw Util.kill("Couldn't parse horizon properties, exiting", e);
        } catch (Throwable thrown) {
            throw Util.kill("Couldn't parse horizon properties, exiting", thrown);
        }
    }

    private static @NonNull List<File> extractExtraPlugins(@NonNull ObjectTree tree, String @NonNull [] args) {
        List<File> initial = new ArrayList<>();

        ObjectArray extraPluginsArray = tree.getArray("extraPlugins");
        for (int i = 0; i < extraPluginsArray.size(); i++) {
            String path = extraPluginsArray.get(i).asString();
            initial.add(new File(path));
        }

        List<File> addedFromArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("--add-plugin=")) {
                addedFromArgs.add(new File(arg.substring("--add-plugin=".length())));
                continue;
            }
            if (arg.startsWith("--add-extra-plugin-jar=")) {
                addedFromArgs.add(new File(arg.substring("--add-extra-plugin-jar=".length())));
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
}
