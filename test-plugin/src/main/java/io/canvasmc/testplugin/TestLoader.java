package io.canvasmc.testplugin;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.tinylog.Logger;

public class TestLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        Logger.info("AFSDJKFLJDJOFIU($*U(*@#*(*@#$@@%$");
    }
}
