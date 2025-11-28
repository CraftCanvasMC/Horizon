package io.canvasmc.horizon;

import joptsimple.OptionSet;

import java.lang.instrument.Instrumentation;

import static io.canvasmc.horizon.Main.LOGGER;

public class Horizon {
    private final OptionSet options;
    private final String version;
    private final Instrumentation instrumentation;

    public Horizon(OptionSet options, String version, Instrumentation instrumentation, String[] providedArgs) {
        this.options = options;
        this.version = version;
        this.instrumentation = instrumentation;
        main(providedArgs);
    }

    public OptionSet getOptions() {
        return options;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public String getVersion() {
        return version;
    }

    private void main(String[] providedArgs) {
        LOGGER.info("Preparing Minecraft server");
        // Note: this should in general act similar to a Paperclip jar
        // TODO - mod loading first, prepare "containers"
    }
}
