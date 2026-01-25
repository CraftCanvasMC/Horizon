package io.canvasmc.horizon.inject.mixin.commandinject;

import com.mojang.brigadier.context.CommandContext;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.papermc.paper.command.PaperPluginsCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.TreeMap;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PaperPluginsCommand.class)
public abstract class PaperPluginsCommandMixin {

    @Unique
    private static final TextColor HORIZON_COLOR = TextColor.color(222, 49, 239);
    @Shadow
    @Final
    private static Component INFO_ICON_SERVER_PLUGIN;

    @Shadow
    private static <T> List<Component> formatProviders(TreeMap<String, PluginProvider<T>> plugins) {
        return List.of();
    }

    @Shadow
    private static Component header(String header, int color, int count, boolean showSize) {
        return Component.empty();
    }

    /**
     * @author dueris
     * @reason Add Horizon plugins to the plugins command without greatly overcomplicating the mixins required
     */
    @Overwrite
    private int execute(@NonNull CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        final TreeMap<String, PluginProvider<JavaPlugin>> horizonPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final TreeMap<String, PluginProvider<JavaPlugin>> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final TreeMap<String, PluginProvider<JavaPlugin>> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        final List<String> horizonPluginJars = HorizonLoader.getInstance().getPlugins().getAll().stream()
            .map(HorizonPlugin::pluginMetadata)
            .map(HorizonMetadata::name)
            .map(String::toLowerCase)
            .toList();

        for (PluginProvider<JavaPlugin> provider : LaunchEntryPointHandler.INSTANCE.get(Entrypoint.PLUGIN).getRegisteredProviders()) {
            PluginMeta configuration = provider.getMeta();
            if (horizonPluginJars.contains(configuration.getName().toLowerCase())) {
                horizonPlugins.put(configuration.getDisplayName(), provider);
            }
            else if (provider instanceof SpigotPluginProvider) {
                spigotPlugins.put(configuration.getDisplayName(), provider);
            }
            else if (provider instanceof PaperPluginParent.PaperServerPluginProvider) {
                paperPlugins.put(configuration.getDisplayName(), provider);
            }
        }

        int sizeHorizonPlugins = horizonPlugins.size();
        int sizePaperPlugins = paperPlugins.size();
        int sizeSpigotPlugins = spigotPlugins.size();
        int sizePlugins = sizeHorizonPlugins + sizePaperPlugins + sizeSpigotPlugins;
        boolean hasAllPluginTypes = sizeHorizonPlugins > 0 && sizePaperPlugins > 0 && sizeSpigotPlugins > 0;
        Component infoMessage = Component.text().append(INFO_ICON_SERVER_PLUGIN).append(Component.text("Server Plugins (%s):".formatted(sizePlugins), NamedTextColor.WHITE)).build();
        sender.sendMessage(infoMessage);

        if (!horizonPlugins.isEmpty()) {
            sender.sendMessage(header("Horizon Plugins", HORIZON_COLOR.value(), sizeHorizonPlugins, hasAllPluginTypes));
        }

        for (Component component : formatProviders(horizonPlugins)) {
            sender.sendMessage(component);
        }

        if (!paperPlugins.isEmpty()) {
            sender.sendMessage(header("Paper Plugins", 166097, sizePaperPlugins, hasAllPluginTypes));
        }

        for (Component component : formatProviders(paperPlugins)) {
            sender.sendMessage(component);
        }

        if (!spigotPlugins.isEmpty()) {
            sender.sendMessage(header("Bukkit Plugins", 15565062, sizeSpigotPlugins, hasAllPluginTypes));
        }

        for (Component component : formatProviders(spigotPlugins)) {
            sender.sendMessage(component);
        }

        return 1;
    }
}
