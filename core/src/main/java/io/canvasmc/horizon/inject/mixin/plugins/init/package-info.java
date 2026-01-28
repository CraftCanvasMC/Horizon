/**
 * <h2>initfix package</h2>
 * <p>
 * This package is for fixing plugin initialization for Paper and Spigot/Bukkit plugins. The classloaders responsible
 * for loading these plugins have changed: since plugin classes are visible to a higher classloader, they are loaded by
 * that classloader. This causes issues because both Paper and Spigot validate that the
 * {@link org.bukkit.plugin.java.JavaPlugin} class is loaded by the correct plugin-specific classloader. When Horizon
 * loads all plugins, they instead end up being loaded by a higher-level classloader.
 * </p>
 *
 * <ul>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.JavaPluginMixin}</li>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.ModernPluginLoadingStrategyMixin}</li>
 * </ul>
 *
 * <p>Paper plugins (paper-plugin.yml)</p>
 * <ul>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.paper.PaperPluginParentMixin}</li>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.paper.PaperServerPluginProviderMixin}</li>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.paper.PaperClasspathBuilderMixin}</li>
 * </ul>
 *
 * <p>Spigot/Bukkit plugins (plugin.yml)</p>
 * <ul>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.spigot.PluginClassLoaderMixin}</li>
 *   <li>{@link io.canvasmc.horizon.inject.mixin.plugins.init.spigot.SpigotPluginProviderMixin}</li>
 * </ul>
 *
 * @author dueris
 */
package io.canvasmc.horizon.inject.mixin.plugins.init;