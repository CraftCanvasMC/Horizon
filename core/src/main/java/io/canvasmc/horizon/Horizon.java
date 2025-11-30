package io.canvasmc.horizon;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.canvasmc.horizon.plugin.EntrypointLoader;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.data.HorizonMetadataDeserializer;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import joptsimple.OptionSet;
import org.tinylog.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * The main class for Horizon that acts as a base
 * that runs the full startup and bootstrap process
 */
public class Horizon {
    public static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(HorizonMetadata.class, new HorizonMetadataDeserializer())
        .create();

    public static Horizon INSTANCE;

    private final OptionSet options;
    private final String version;
    private final Instrumentation instrumentation;
    private List<HorizonPlugin> plugins;

    public Horizon(OptionSet options, String version, Instrumentation instrumentation, String[] providedArgs) {
        this.options = options;
        this.version = version;
        this.instrumentation = instrumentation;
        INSTANCE = this;
        try {
            start(providedArgs);
        } catch (Throwable thrown) {
            Logger.error("Couldn't start Horizon server due to an unexpected exception!");
            Logger.error(thrown);
            System.exit(1);
        }
    }

    /**
     * The optionset parsed for Horizon
     *
     * @return the Horizon optionset
     * @see OptionSet
     */
    public OptionSet getOptions() {
        return options;
    }

    /**
     * Gets the JVM instrumentation
     *
     * @return the instrumentation
     * @see Instrumentation
     */
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * Gets the version of Horizon this is
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get all the plugins in the Horizon server
     * <p>
     * <b>Note:</b> if plugins aren't loaded yet, the list will be empty
     * </p>
     *
     * @return all plugins
     */
    public List<HorizonPlugin> getPlugins() {
        return this.plugins == null ? List.of() : this.plugins;
    }

    /**
     * Starts the Horizon server
     *
     * @param providedArgs the arguments provided to the server to be passed to the Minecraft main method
     */
    private void start(String[] providedArgs) {
        // Note: this should in general act similar to a Paperclip jar
        Logger.info("Preparing Minecraft server");
        // TODO - try and build paperclip jar, download?
        this.plugins = ImmutableList.copyOf(EntrypointLoader.init());
    }
}
