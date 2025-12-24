package io.canvasmc.horizon;

import com.google.gson.GsonBuilder;
import io.canvasmc.horizon.instrument.JvmAgent;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static io.canvasmc.horizon.Horizon.LOGGER;

public class Main {

    public static void main(String[] args) {
        // TODO - can we support this..?
        if (Boolean.getBoolean("paper.useLegacyPluginLoading")) {
            throw new IllegalStateException("Legacy plugin loading is unsupported with Horizon");
        }
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
        LOGGER.debug("Metadata:\n{}", new GsonBuilder().setPrettyPrinting().create().toJson(metadata));
        LOGGER.debug("Launch args: {}", Arrays.toString(args));

        // load properties and start horizon init
        new Horizon(ServerProperties.load(args), metadata.get("Implementation-Version").toString(), JvmAgent.INSTRUMENTATION, args);
    }
}
