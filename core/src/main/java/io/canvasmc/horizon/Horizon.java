package io.canvasmc.horizon;

import joptsimple.OptionSet;
import org.tinylog.Logger;

import java.lang.instrument.Instrumentation;

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
        Logger.info("Preparing Minecraft server");
        // Note: this should in general act similar to a Paperclip jar
        // TODO - mod loading first, prepare "containers"
        // TODO - try and build paperclip jar, download?
    }
}
