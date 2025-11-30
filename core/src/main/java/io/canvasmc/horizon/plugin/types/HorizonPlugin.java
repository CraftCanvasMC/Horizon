package io.canvasmc.horizon.plugin.types;

import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.util.FileJar;

/**
 * Represents a full valid and parsed Horizon plugin data
 * <p>
 * <b>Note:</b> this guarantees all provided values are correct, except it does not validate contents of some files, as
 * this is validated at a later time in the boot process
 * </p>
 */
public record HorizonPlugin(String identifier, FileJar file, HorizonMetadata pluginMetadata) {
}
