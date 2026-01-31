package io.canvasmc.horizon.inject.mixin.plugins;

import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.type.PluginFileType;
import io.papermc.paper.plugin.provider.type.PluginTypeFactory;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.InputStreamReader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mixin(PluginFileType.class)
public class PluginFileTypeMixin<T, C extends PluginMeta> {
    @Unique
    private static final String HORIZON_PLUGIN_JSON = "horizon.plugin.json";
    @Shadow
    @Final
    private static List<PluginFileType<?, ?>> VALUES;
    @Shadow
    @Final
    private PluginTypeFactory<T, C> factory;
    @Shadow
    @Final
    public String config;

    /**
     * @author dueris
     * @reason inject horizon hybrid capabilities
     */
    @Overwrite
    public static @Nullable PluginFileType<?, ?> guessType(JarFile file) {
        JarEntry entry;
        try {
            if ((entry = file.getJarEntry(HORIZON_PLUGIN_JSON)) != null) {
                ObjectTree compiled = ObjectTree.read()
                    .format(Format.JSON)
                    .from(new InputStreamReader(file.getInputStream(entry)));
                if (compiled.getValueSafe("is_hybrid").asBooleanOptional().orElse(false)) {
                    return PluginFileType.PAPER;
                }
            }
        } catch (Throwable thrown) {
            throw new RuntimeException("InStream couldn't be read", thrown);
        }
        for (PluginFileType<?, ?> type : VALUES) {
            entry = file.getJarEntry(type.config);
            if (entry != null) {
                return type;
            }
        }

        return null;
    }

    /**
     * @author dueris
     * @reason inject horizon hybrid capabilities
     */
    @Overwrite
    public C getConfig(@NonNull JarFile file) throws Exception {
        JarEntry entry = file.getJarEntry(HORIZON_PLUGIN_JSON);
        if (entry == null) {
            entry = file.getJarEntry(this.config);
        }
        return this.factory.create(file, entry);
    }
}
