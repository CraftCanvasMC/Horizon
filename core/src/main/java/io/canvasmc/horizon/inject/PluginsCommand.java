package io.canvasmc.horizon.inject;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.ProviderStatus;
import io.papermc.paper.plugin.provider.ProviderStatusHolder;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings("UnstableApiUsage")
public class PluginsCommand {
    private static final TextColor INFO_COLOR = TextColor.color(52, 159, 218);
    private static final TextColor HORIZON_COLOR = TextColor.color(222, 49, 239);

    private static final Component SERVER_PLUGIN_INFO = Component.text("ℹ What is a server plugin?", INFO_COLOR).append(asPlainComponents("Server plugins can add new behavior to your server!\nYou can find new plugins on Paper's plugin repository, Hangar.\n\nhttps://hangar.papermc.io/\n"));
    private static final Component LEGACY_PLUGIN_INFO = Component.text("ℹ What is a legacy plugin?", INFO_COLOR).append(asPlainComponents("A legacy plugin is a plugin that was made on\nvery old unsupported versions of the game.\n\nIt is encouraged that you replace this plugin,\nas they might not work in the future and may cause\nperformance issues.\n"));
    private static final Component LEGACY_PLUGIN_STAR = Component.text('*', TextColor.color(255, 212, 42)).hoverEvent(LEGACY_PLUGIN_INFO);

    private static final Component INFO_ICON_START = Component.text("ℹ ", INFO_COLOR);
    private static final Component INFO_ICON_SERVER_PLUGIN = INFO_ICON_START.hoverEvent(SERVER_PLUGIN_INFO).clickEvent(ClickEvent.openUrl("https://docs.papermc.io/paper/adding-plugins"));

    private static final Component PLUGIN_TICK = Component.text("- ", NamedTextColor.DARK_GRAY);
    private static final Component PLUGIN_TICK_EMPTY = Component.text(" ");

    private static final Type JAVA_PLUGIN_PROVIDER_TYPE = (new TypeToken<PluginProvider<JavaPlugin>>() {
    }).getType();

    public static LiteralCommandNode<CommandSourceStack> create() {
        PluginsCommand command = new PluginsCommand();
        LiteralArgumentBuilder<CommandSourceStack> argBuilder = literal("plugins")
            .requires((source) -> source.getSender().hasPermission("bukkit.command.plugins"));
        return argBuilder.executes(command::execute).build();
    }

    private static <T> @NonNull List<Component> formatProviders(@NonNull TreeMap<String, PluginProvider<T>> plugins) {
        List<Component> components = new ArrayList<>(plugins.size());

        for (PluginProvider<T> entry : plugins.values()) {
            components.add(formatProvider(entry));
        }

        boolean isFirst = true;
        List<Component> formattedSubLists = new ArrayList<>();

        for (List<Component> componentSublist : Lists.partition(components, 10)) {
            Component component = Component.space();
            if (isFirst) {
                component = component.append(PLUGIN_TICK);
                isFirst = false;
            } else {
                component = PLUGIN_TICK_EMPTY;
            }

            formattedSubLists.add(component.append(Component.join(JoinConfiguration.commas(true), componentSublist)));
        }

        return formattedSubLists;
    }

    private static @NonNull Component formatProvider(PluginProvider<?> provider) {
        TextComponent.Builder builder = Component.text();
        if (provider instanceof SpigotPluginProvider spigotPluginProvider) {
            if (CraftMagicNumbers.isLegacy(spigotPluginProvider.getMeta())) {
                builder.append(LEGACY_PLUGIN_STAR);
            }
        }

        String name = provider.getMeta().getName();
        Component pluginName = Component.text(name, fromStatus(provider)).clickEvent(ClickEvent.runCommand("/version " + name));
        builder.append(pluginName);
        return builder.build();
    }

    private static @NonNull Component header(String header, int color, int count, boolean showSize) {
        TextComponent.Builder componentHeader = Component.text().color(TextColor.color(color)).append(Component.text(header));
        if (showSize) {
            componentHeader.appendSpace().append(Component.text("(" + count + ")"));
        }

        return componentHeader.append(Component.text(":")).build();
    }

    private static @NonNull Component asPlainComponents(@NonNull String strings) {
        TextComponent.Builder builder = Component.text();

        for (String string : strings.split("\n")) {
            builder.append(Component.newline());
            builder.append(Component.text(string, NamedTextColor.WHITE));
        }

        return builder.build();
    }

    private static TextColor fromStatus(PluginProvider<?> provider) {
        if (provider instanceof ProviderStatusHolder statusHolder) {
            if (statusHolder.getLastProvidedStatus() != null) {
                ProviderStatus status = statusHolder.getLastProvidedStatus();
                if (status == ProviderStatus.INITIALIZED && GenericTypeReflector.isSuperType(JAVA_PLUGIN_PROVIDER_TYPE, provider.getClass())) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(provider.getMeta().getName());
                    if (plugin == null) {
                        return NamedTextColor.RED;
                    }

                    return plugin.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED;
                }

                NamedTextColor var10000;
                switch (status) {
                    case INITIALIZED -> var10000 = NamedTextColor.GREEN;
                    case ERRORED -> var10000 = NamedTextColor.RED;
                    default -> throw new MatchException(null, null);
                }

                return var10000;
            }
        }

        if (provider instanceof PaperPluginParent.PaperServerPluginProvider serverPluginProvider) {
            if (serverPluginProvider.shouldSkipCreation()) {
                return NamedTextColor.RED;
            }
        }

        return NamedTextColor.RED;
    }

    private int execute(@NonNull CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        final TreeMap<String, PluginProvider<JavaPlugin>> horizonPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final TreeMap<String, PluginProvider<JavaPlugin>> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final TreeMap<String, PluginProvider<JavaPlugin>> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        final List<String> horizonPluginJars = Horizon.INSTANCE.getPlugins().getAll().stream()
            .map(HorizonPlugin::pluginMetadata)
            .map(HorizonMetadata::name)
            .map(String::toLowerCase)
            .toList();

        for (PluginProvider<JavaPlugin> provider : LaunchEntryPointHandler.INSTANCE.get(Entrypoint.PLUGIN).getRegisteredProviders()) {
            PluginMeta configuration = provider.getMeta();
            if (horizonPluginJars.contains(configuration.getName().toLowerCase())) {
                horizonPlugins.put(configuration.getDisplayName(), provider);
            } else if (provider instanceof SpigotPluginProvider) {
                spigotPlugins.put(configuration.getDisplayName(), provider);
            } else if (provider instanceof PaperPluginParent.PaperServerPluginProvider) {
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
