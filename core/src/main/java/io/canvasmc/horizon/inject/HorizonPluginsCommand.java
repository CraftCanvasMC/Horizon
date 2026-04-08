package io.canvasmc.horizon.inject;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HorizonPluginsCommand {

    private static Component appendPlugins(final @NonNull Map<Type, List<Component>> plugins, final Type type) {
        Component msg = Component.empty();
        List<Component> get = plugins.get(type);
        for (int i = 0; i < get.size(); i++) {
            final Component component = get.get(i);
            msg = msg.append(component);
            if (i != get.size() - 1) {
                msg = msg.append(Component.text(", ").color(NamedTextColor.DARK_GRAY));
            }
        }
        return msg;
    }

    public static LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("plugins").executes((context) -> {
            CommandSourceStack source = context.getSource();
            Map<Type, List<Component>> plugins = new HashMap<>();
            plugins.put(Type.HORIZON, new ArrayList<>());
            plugins.put(Type.PAPER, new ArrayList<>());
            plugins.put(Type.SPIGOT, new ArrayList<>());
            for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                Component pluignComponent = Component.text(plugin.getName()).color(plugin.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED);
                if (plugin.getPluginMeta() instanceof PaperPluginMeta) {
                    plugins.get(Type.PAPER).add(pluignComponent);
                }
                else plugins.get(Type.SPIGOT).add(pluignComponent);
            }
            for (final HorizonPlugin horizonPlugin : HorizonLoader.getInstance().getPlugins().getAll()) {
                plugins.get(Type.HORIZON).add(Component.text(horizonPlugin.pluginMetadata().name()).color(NamedTextColor.GREEN));
            }
            source.getSender().sendMessage((source.getSender() instanceof ConsoleCommandSender ? Component.newline() : Component.empty())
                .append(Component.text("Horizon Plugins:").color(NamedTextColor.LIGHT_PURPLE))
                .appendNewline()
                .append(Component.text("- [").color(NamedTextColor.DARK_GRAY))
                .append(appendPlugins(plugins, Type.HORIZON))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("Paper Plugins:").color(NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text("- [").color(NamedTextColor.DARK_GRAY))
                .append(appendPlugins(plugins, Type.PAPER))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("Spigot Plugins:").color(NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("- [").color(NamedTextColor.DARK_GRAY))
                .append(appendPlugins(plugins, Type.SPIGOT))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY)));
            return 0;
        }).build();
    }

    private enum Type {
        HORIZON, PAPER, SPIGOT
    }
}
