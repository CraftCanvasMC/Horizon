package io.canvasmc.horizon.inject.mixin.plugins;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.ParseException;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mixin(targets = "io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProviderFactory")
public class SpigotPluginProviderFactoryMixin {

    /**
     * @author dueris
     * @reason remap the horizon JSON to YAML
     */
    @Overwrite
    public PluginDescriptionFile create(@NonNull JarFile file, @NonNull JarEntry config) throws InvalidDescriptionException {
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
            return new PluginDescriptionFile(inputStream);
        } catch (YAMLException | IOException | ParseException ex) {
            throw new InvalidDescriptionException(ex);
        }
    }
}
