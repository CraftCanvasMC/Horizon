package io.canvasmc.horizon.plugin;

import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.FileJar;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record PluginTree(List<PluginNode> roots) {

    @Contract("_ -> new")
    public static @NonNull PluginTree from(HorizonPlugin @NonNull [] plugins) {
        List<PluginNode> roots = new ArrayList<>();

        for (HorizonPlugin plugin : plugins) {
            roots.add(PluginNode.from(plugin));
        }

        return new PluginTree(roots);
    }

    public @NonNull String format() {
        StringBuilder builder = new StringBuilder();
        AtomicInteger count = new AtomicInteger();
        builder.append("Found ").append(roots.size()).append(" plugin(s):\n");

        for (int i = 0; i < roots.size(); i++) {
            PluginNode root = roots.get(i);
            boolean isLast = i == roots.size() - 1;

            builder.append("\t- ")
                .append(root.name)
                .append(" ")
                .append(root.version)
                .append("\n");

            if (!root.children.isEmpty()) {
                formatChildren(builder, root.children, "\t   ", isLast, count);
            }

            count.incrementAndGet();
        }

        return builder.toString().trim();
    }

    private void formatChildren(StringBuilder builder, @NonNull List<PluginNode> children, String prefix, boolean parentIsLast, AtomicInteger count) {
        for (int i = 0; i < children.size(); i++) {
            PluginNode child = children.get(i);
            boolean isLast = i == children.size() - 1;

            builder.append(prefix)
                .append(isLast ? "\\-- " : "|-- ")
                .append(child.name);

            if (child.type == NodeType.HORIZON_PLUGIN) {
                builder.append(" ").append(child.version);
                count.incrementAndGet();
            }

            builder.append("\n");

            if (!child.children.isEmpty()) {
                String childPrefix = prefix + (isLast ? "    " : "|   ");
                formatChildren(builder, child.children, childPrefix, isLast, count);
            }
        }
    }

    public @NonNull List<HorizonPlugin> getAll() {
        List<HorizonPlugin> plugins = new ArrayList<>();

        for (PluginNode root : roots) {
            collectPlugins(root, plugins);
        }

        return plugins;
    }

    private void collectPlugins(@NonNull PluginNode node, List<HorizonPlugin> plugins) {
        if (node.type == NodeType.HORIZON_PLUGIN && node.plugin != null) {
            plugins.add(node.plugin);
        }

        for (PluginNode child : node.children) {
            collectPlugins(child, plugins);
        }
    }

    public enum NodeType {
        HORIZON_PLUGIN,
        SERVER_PLUGIN,
        LIBRARY
    }

    public record PluginNode(String name, String version, NodeType type, List<PluginNode> children,
                             @Nullable HorizonPlugin plugin) {

        @Contract("_ -> new")
        public static @NonNull PluginNode from(@NonNull HorizonPlugin plugin) {
            String name = plugin.pluginMetadata().name();
            String version = plugin.pluginMetadata().version();
            List<PluginNode> children = new ArrayList<>();

            HorizonPlugin.NestedData nestedData = plugin.nestedData();

            for (HorizonPlugin nested : nestedData.horizonEntries()) {
                children.add(from(nested));
            }

            for (FileJar serverPlugin : nestedData.serverPluginEntries()) {
                String jarName = serverPlugin.ioFile().getName().replace(".jar", "");
                children.add(new PluginNode(jarName, "unknown", NodeType.SERVER_PLUGIN, List.of(), null));
            }

            for (FileJar library : nestedData.libraryEntries()) {
                String jarName = library.ioFile().getName().replace(".jar", "");
                children.add(new PluginNode(jarName, "unknown", NodeType.LIBRARY, List.of(), null));
            }

            return new PluginNode(name, version, NodeType.HORIZON_PLUGIN, children, plugin);
        }

        @Override
        public @NonNull @UnmodifiableView List<PluginNode> children() {
            return Collections.unmodifiableList(children);
        }
    }
}