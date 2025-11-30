package io.canvasmc.horizon.plugin.types;

import io.canvasmc.horizon.plugin.data.CandidateMetadata;
import io.canvasmc.horizon.util.FileJar;

/**
 * Represents a candidate for horizon plugin creation
 *
 * @param fileJar          the io and jar files
 * @param horizonEntryName the entry name in the jar file that points to the horizon metadata
 * @param metadata         plugin metadata
 */
public record PluginCandidate(FileJar fileJar, CandidateMetadata metadata) {
}