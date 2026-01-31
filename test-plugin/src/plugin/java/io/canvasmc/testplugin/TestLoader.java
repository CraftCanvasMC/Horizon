package io.canvasmc.testplugin;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public class TestLoader implements PluginLoader {

    @Override
    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
        System.out.println("Test plugin split source LOADER phase");
    }
}
