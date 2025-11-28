package io.canvasmc.horizon;

import io.canvasmc.horizon.instrument.JvmAgent;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.util.Arrays.asList;

public class Main {
    static final Logger LOGGER = LoggerFactory.getLogger("Horizon");

    public static void main(String[] args) {
        LOGGER.info("Building Horizon version metadata...");
        Map<String, Object> metadata = new HashMap<>();
        try {
            //noinspection resource
            JarFile sourceJar = new JarFile(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            Manifest manifest = sourceJar.getManifest();
            for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't fetch source jar", e);
        }

        OptionParser parser = new OptionParser() {
            {
                this.acceptsAll(asList("?", "help"), "Show the help");

                this.acceptsAll(asList("P", "plugins"), "Plugin directory to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("plugins"))
                        .describedAs("Plugin directory");

                this.acceptsAll(asList("add-plugin", "add-extra-plugin-jar"), "Specify paths to extra plugin jars to be loaded in addition to those in the plugins folder. This argument can be specified multiple times, once for each extra plugin jar path.")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File[]{})
                        .describedAs("Jar file");
            }
        };

        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (OptionException ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }

        if ((options == null) || (options.has("?"))) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ex) {
                LOGGER.error("An unexpected error occurred", ex);
            }
            return;
        }

        new Horizon(options, metadata.get("Implementation-Version").toString(), JvmAgent.INSTRUMENT, args);
    }
}
