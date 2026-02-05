package io.canvasmc.horizon.plugin;

import java.io.File;

/**
 * Represents the load context for the plugin loader
 *
 * @param pluginsDirectory
 *     root directory for plugin searching
 * @param cacheDirectory
 *     the directory for Horizon IO cache, like for nested jars
 *
 * @author dueris
 */
public record LoadContext(File pluginsDirectory, File cacheDirectory) {}