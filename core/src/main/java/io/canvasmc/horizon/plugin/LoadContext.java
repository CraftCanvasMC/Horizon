package io.canvasmc.horizon.plugin;

import java.io.File;

/**
 * Represents the load context for the plugin loader
 *
 * @param pluginsDirectory root directory for plugin searching
 */
public record LoadContext(File pluginsDirectory, File cacheDirectory) {
}