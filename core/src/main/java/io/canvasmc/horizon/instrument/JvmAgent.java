package io.canvasmc.horizon.instrument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

public class JvmAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Instrumentation");
    // Note: non-null guaranteed when accessed outside this class
    public static Instrumentation INSTRUMENT;

    public static void agentmain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        LOGGER.info("Booted from agent main");
        INSTRUMENT = inst;
    }
}
