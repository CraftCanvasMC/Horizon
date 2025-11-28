package io.canvasmc.horizon.instrument;

import org.tinylog.Logger;

import java.lang.instrument.Instrumentation;

public class JvmAgent {
    // Note: non-null guaranteed when accessed outside this class
    public static Instrumentation INSTRUMENT;

    public static void agentmain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        Logger.info("Booted from agent main");
        INSTRUMENT = inst;
    }
}
