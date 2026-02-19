package io.canvasmc.horizon.plugin;

import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.FileJar;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The stored plugin tree created by the Horizon plugin loader, aka {@link io.canvasmc.horizon.MixinPluginLoader}. This
 * represents a tree-like data structure of Horizon plugins, the internal plugin, and all nested entries too.
 *
 * @param roots
 *     the root plugin node(s)
 *
 * @author dueris
 */
public record PluginTree(List<PluginNode> roots) {

    @ApiStatus.Internal
    @Contract("_ -> new")
    public static @NonNull PluginTree from(HorizonPlugin @NonNull [] plugins) {
        List<PluginNode> roots = new ArrayList<>();

        for (HorizonPlugin plugin : plugins) {
            roots.add(PluginNode.from(plugin));
        }

        return new PluginTree(roots);
    }

    /**
     * Formats the tree into a readable printed structure, like this:
     * <pre>
     * {@code Found 2 plugin(s):
     * 	- Example 1.0.0
     * 	   |-- WowAnotherExample 2.3.1
     * 	   |   \-- SuperFastLib
     * 	   |-- Vault
     * 	   \-- SuperEpicThingie
     * 	- AnotherCoolThing 0.9.4}
     * </pre>
     *
     * @return the readable pretty-printed structure
     */
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

    /**
     * Collects and returns all Horizon plugins within the tree, including nested entries
     *
     * @return all Horizon plugins
     */
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

    /**
     * The type of node this is
     *
     * @author dueris
     */
    public enum NodeType {
        HORIZON_PLUGIN,
        SERVER_PLUGIN,
        LIBRARY
    }

    /**
     * The plugin node in the tree
     *
     * @param name
     *     the name of the node
     * @param version
     *     the version of the node
     * @param type
     *     the type of node
     * @param children
     *     the children nodes of this node, can be empty
     * @param plugin
     *     the <b>nullable</b> Horizon plugin instance associated with this entry. If this node isn't a Horizon plugin,
     *     this will be null
     *
     * @author dueris
     */
    public record PluginNode(
        String name, String version, NodeType type, List<PluginNode> children,
        @Nullable HorizonPlugin plugin) {

        @Contract("_ -> new")
        public static @NonNull PluginNode from(@NonNull HorizonPlugin plugin) {
            String name = plugin.pluginMetadata().name();
            String version = plugin.pluginMetadata().version();
            List<PluginNode> children = new ArrayList<>();

            HorizonPlugin.CompiledNestedPlugins nestedData = plugin.nestedData();

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