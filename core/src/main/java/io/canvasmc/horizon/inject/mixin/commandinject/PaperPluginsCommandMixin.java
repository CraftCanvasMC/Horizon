package io.canvasmc.horizon.inject.mixin.commandinject;

import com.mojang.brigadier.context.CommandContext;
import io.canvasmc.horizon.HorizonLoader;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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
    @Final
    private static Component PLUGIN_TICK;

    @Shadow
    private static Component header(String header, int color, int count, boolean showSize) {
        return Component.empty();
    }

    @Shadow
    private static <T> List<Component> formatProviders(TreeMap<String, PluginProvider<T>> plugins) {
        return List.of();
    }

    /**
     * @author dueris
     * @reason Add Horizon plugins to the plugins command without greatly overcomplicating the mixins required
     */
    @Overwrite
    private int execute(@NonNull CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        final TreeMap<String, PluginProvider<JavaPlugin>> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final TreeMap<String, PluginProvider<JavaPlugin>> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final List<HorizonPlugin> horizonPlugins = HorizonLoader.getInstance().getPlugins().getAll();

        for (PluginProvider<JavaPlugin> provider : LaunchEntryPointHandler.INSTANCE.get(Entrypoint.PLUGIN).getRegisteredProviders()) {
            PluginMeta configuration = provider.getMeta();
            if (provider instanceof SpigotPluginProvider) {
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
            Component built = PLUGIN_TICK;
            built = built.append(Component.text("[", NamedTextColor.DARK_GRAY));
            for (int i = 0; i < horizonPlugins.size(); i++) {
                built = built.append(Component.text(horizonPlugins.get(i).pluginMetadata().name(), NamedTextColor.GREEN));
                if (i != horizonPlugins.size() - 1) {
                    // add comma
                    built = built.append(Component.text(", ", NamedTextColor.DARK_GRAY));
                }
            }
            built = built.append(Component.text("]", NamedTextColor.DARK_GRAY));
            sender.sendMessage(built);
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
