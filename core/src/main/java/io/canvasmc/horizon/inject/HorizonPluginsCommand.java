package io.canvasmc.horizon.inject;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class HorizonPluginsCommand {
    private static final Set<String> RESERVED_DEPENDENCY_KEYS = Set.of("minecraft", "java", "asm");

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

    private static int executeOverview(final @NonNull CommandSourceStack source) {
        Map<Type, List<Component>> plugins = new HashMap<>();
        plugins.put(Type.HORIZON, new ArrayList<>());
        plugins.put(Type.PAPER, new ArrayList<>());
        plugins.put(Type.SPIGOT, new ArrayList<>());

        final Map<String, HorizonPlugin> providers = providersByIdentifier();
        final Set<String> bundledMembers = new LinkedHashSet<>();
        final Set<Path> bundledServerPlugins = new LinkedHashSet<>();
        for (final HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
            if (plugin.isBundle()) {
                bundledMembers.addAll(collectBundleChildren(plugin, providers).stream()
                    .map(child -> child.pluginMetadata().id())
                    .toList());
                bundledServerPlugins.addAll(collectBundleServerPluginPaths(plugin, providers));
            }
        }

        for (final HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
            if (bundledMembers.contains(plugin.pluginMetadata().id())) {
                continue;
            }
            plugins.get(Type.HORIZON).add(bundleAwareName(plugin));
        }

        for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            final Optional<Path> pluginPath = loadedPluginPath(plugin);
            if (pluginPath.isPresent() && bundledServerPlugins.contains(pluginPath.get())) {
                continue;
            }

            final Component pluginComponent = pluginName(plugin);
            if (plugin.getPluginMeta() instanceof PaperPluginMeta) {
                plugins.get(Type.PAPER).add(pluginComponent);
            }
            else {
                plugins.get(Type.SPIGOT).add(pluginComponent);
            }
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
    }

    private static int executeBundleLookup(final @NonNull CommandSourceStack source, final @NonNull String identifier) {
        final Optional<HorizonPlugin> plugin = findByIdentifier(identifier);
        if (plugin.isEmpty()) {
            source.getSender().sendMessage(Component.text("Unknown Horizon plugin: " + identifier).color(NamedTextColor.RED));
            return 0;
        }

        final HorizonPlugin horizonPlugin = plugin.get();
        if (!horizonPlugin.isBundle()) {
            source.getSender().sendMessage(Component.text(horizonPlugin.pluginMetadata().name() + " is not a bundle").color(NamedTextColor.YELLOW));
            return 0;
        }

        final List<Component> subplugins = new ArrayList<>();
        final Map<String, HorizonPlugin> providers = providersByIdentifier();
        for (final HorizonPlugin child : collectBundleChildren(horizonPlugin, providers)) {
            subplugins.add(bundleAwareName(child));
        }
        final Map<Path, Plugin> loadedPlugins = loadedPluginsByPath();
        for (final Path child : collectBundleServerPluginPaths(horizonPlugin, providers)) {
            subplugins.add(bundleAwareServerPluginName(child, loadedPlugins));
        }

        source.getSender().sendMessage((source.getSender() instanceof ConsoleCommandSender ? Component.newline() : Component.empty())
            .append(Component.text(horizonPlugin.pluginMetadata().name() + "*").color(NamedTextColor.LIGHT_PURPLE))
            .appendNewline()
            .append(Component.text("id: ").color(NamedTextColor.DARK_GRAY))
            .append(Component.text(horizonPlugin.pluginMetadata().id()).color(NamedTextColor.GRAY))
            .appendNewline()
            .append(Component.text("subplugins: ").color(NamedTextColor.DARK_GRAY))
            .append(subplugins.isEmpty()
                ? Component.text("none").color(NamedTextColor.GRAY)
                : appendComponents(subplugins))
        );
        return 0;
    }

    private static @NonNull List<HorizonPlugin> collectBundleChildren(
        final @NonNull HorizonPlugin plugin,
        final @NonNull Map<String, HorizonPlugin> providers
    ) {
        final List<HorizonPlugin> collected = new ArrayList<>();
        final Set<String> visited = new LinkedHashSet<>();
        visited.add(plugin.pluginMetadata().id());
        collectBundleChildren(plugin, providers, visited, collected);
        return collected;
    }

    private static void collectBundleChildren(
        final @NonNull HorizonPlugin plugin,
        final @NonNull Map<String, HorizonPlugin> providers,
        final @NonNull Set<String> visited,
        final @NonNull List<HorizonPlugin> collected
    ) {
        for (final HorizonPlugin child : plugin.nestedData().horizonEntries()) {
            if (child.pluginMetadata().id().equals(plugin.pluginMetadata().id())) {
                continue;
            }
            if (!visited.add(child.pluginMetadata().id())) {
                continue;
            }
            collected.add(child);
            collectBundleChildren(child, providers, visited, collected);
        }

        for (final String dependencyId : pluginDependencyIds(plugin.pluginMetadata().dependencies())) {
            final HorizonPlugin child = providers.get(dependencyId);
            if (child == null || child.pluginMetadata().id().equals(plugin.pluginMetadata().id())) {
                continue;
            }
            if (!visited.add(child.pluginMetadata().id())) {
                continue;
            }
            collected.add(child);
            collectBundleChildren(child, providers, visited, collected);
        }
    }

    private static @NonNull List<Path> collectBundleServerPluginPaths(
        final @NonNull HorizonPlugin plugin,
        final @NonNull Map<String, HorizonPlugin> providers
    ) {
        final List<Path> collected = new ArrayList<>();
        final Set<String> visited = new LinkedHashSet<>();
        visited.add(plugin.pluginMetadata().id());
        collectBundleServerPluginPaths(plugin, providers, visited, collected);
        return collected;
    }

    private static void collectBundleServerPluginPaths(
        final @NonNull HorizonPlugin plugin,
        final @NonNull Map<String, HorizonPlugin> providers,
        final @NonNull Set<String> visited,
        final @NonNull List<Path> collected
    ) {
        for (final FileJar serverPlugin : plugin.nestedData().serverPluginEntries()) {
            collected.add(serverPlugin.ioFile().toPath().toAbsolutePath().normalize());
        }

        for (final HorizonPlugin child : plugin.nestedData().horizonEntries()) {
            if (child.pluginMetadata().id().equals(plugin.pluginMetadata().id())) {
                continue;
            }
            if (!visited.add(child.pluginMetadata().id())) {
                continue;
            }
            collectBundleServerPluginPaths(child, providers, visited, collected);
        }

        for (final String dependencyId : pluginDependencyIds(plugin.pluginMetadata().dependencies())) {
            final HorizonPlugin child = providers.get(dependencyId);
            if (child == null || child.pluginMetadata().id().equals(plugin.pluginMetadata().id())) {
                continue;
            }
            if (!visited.add(child.pluginMetadata().id())) {
                continue;
            }
            collectBundleServerPluginPaths(child, providers, visited, collected);
        }
    }

    private static Optional<HorizonPlugin> findByIdentifier(final @NonNull String identifier) {
        final String normalized = identifier.toLowerCase();
        return Optional.ofNullable(providersByIdentifier().get(normalized));
    }

    private static Component bundleAwareName(final @NonNull HorizonPlugin plugin) {
        final Component base = Component.text(plugin.pluginMetadata().name()).color(NamedTextColor.GREEN);
        if (!plugin.isBundle()) {
            return base;
        }
        return base.append(Component.text("*").color(NamedTextColor.GRAY));
    }

    private static Component bundleAwareServerPluginName(
        final @NonNull Path serverPluginPath,
        final @NonNull Map<Path, Plugin> loadedPlugins
    ) {
        final Plugin plugin = loadedPlugins.get(serverPluginPath);
        if (plugin != null) {
            return pluginName(plugin);
        }

        final String fileName = serverPluginPath.getFileName() == null ? serverPluginPath.toString() : serverPluginPath.getFileName().toString();
        return Component.text(fileName.replace(".jar", "")).color(NamedTextColor.GRAY);
    }

    private static @NonNull Map<Path, Plugin> loadedPluginsByPath() {
        final Map<Path, Plugin> plugins = new HashMap<>();
        for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            loadedPluginPath(plugin).ifPresent(path -> plugins.put(path, plugin));
        }
        return plugins;
    }

    private static Optional<Path> loadedPluginPath(final @NonNull Plugin plugin) {
        try {
            if (plugin.getClass().getProtectionDomain() == null
                || plugin.getClass().getProtectionDomain().getCodeSource() == null
                || plugin.getClass().getProtectionDomain().getCodeSource().getLocation() == null) {
                return Optional.empty();
            }
            return Optional.of(Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize());
        } catch (final Exception ignored) {
            return Optional.empty();
        }
    }

    private static Component pluginName(final @NonNull Plugin plugin) {
        return Component.text(plugin.getName()).color(plugin.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED);
    }

    private static @NonNull Map<String, HorizonPlugin> providersByIdentifier() {
        final Map<String, HorizonPlugin> providers = new HashMap<>();
        for (final HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
            plugin.pluginMetadata().identifiers().forEach(identifier -> providers.put(identifier, plugin));
        }
        HorizonLoader.getInternalPlugin().pluginMetadata().identifiers()
            .forEach(identifier -> providers.put(identifier, HorizonLoader.getInternalPlugin()));
        return providers;
    }

    private static @NonNull List<String> pluginDependencyIds(final @NonNull ObjectTree dependencies) {
        final List<String> ids = new ArrayList<>();
        for (final String key : dependencies.keys()) {
            if (RESERVED_DEPENDENCY_KEYS.contains(key)) {
                continue;
            }
            if (dependencies.getValueSafe(key).asStringOptional().isPresent()) {
                ids.add(key);
            }
        }
        return ids;
    }

    private static Component appendComponents(final @NonNull List<Component> components) {
        Component msg = Component.empty();
        for (int i = 0; i < components.size(); i++) {
            msg = msg.append(components.get(i));
            if (i != components.size() - 1) {
                msg = msg.append(Component.text(", ").color(NamedTextColor.DARK_GRAY));
            }
        }
        return msg;
    }

    public static LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("plugins")
            .executes((context) -> executeOverview(context.getSource()))
            .then(Commands.argument("plugin", StringArgumentType.word())
                .executes((context) -> executeBundleLookup(
                    context.getSource(),
                    StringArgumentType.getString(context, "plugin")
                )))
            .build();
    }

    private enum Type {
        HORIZON, PAPER, SPIGOT
    }
}
