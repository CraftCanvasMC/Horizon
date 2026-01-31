package io.canvasmc.horizon.inject.mixin.plugins;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mixin(targets = "io.papermc.paper.plugin.provider.type.paper.PaperPluginProviderFactory")
public class PaperPluginProviderFactoryMixin {

    /**
     * @author dueris
     * @reason remap the horizon JSON to YAML
     */
    @Overwrite
    public PaperPluginMeta create(JarFile file, JarEntry config) throws IOException {
        PaperPluginMeta configuration;
        try {
            InputStream inputStream = file.getInputStream(config);
            if (config.getName().equalsIgnoreCase("horizon.plugin.json")) {
                // is horizon plugin, replace in-stream with YAML
                ObjectTree tree = ObjectTree.read().format(Format.JSON).from(inputStream);
                HorizonPlugin plugin = HorizonLoader.getInstance().getPlugins().getAll().stream()
                    .filter(pl -> pl.pluginMetadata().name().equalsIgnoreCase(tree.getValueOrThrow("name").asString()))
                    .findFirst().orElseThrow();
                // plugin found, name matched YAML, encode and replace stream
                inputStream = new ByteArrayInputStream(plugin.pluginMetadata().encodeToYaml().getBytes(StandardCharsets.UTF_8));
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                configuration = PaperPluginMeta.create(bufferedReader);
            }
        } catch (Throwable thrown) {
            throw new IOException(thrown);
        }
        return configuration;
    }
}
