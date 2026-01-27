package io.canvasmc.horizon.plugin.types;

import io.canvasmc.horizon.plugin.data.CandidateMetadata;
import io.canvasmc.horizon.util.FileJar;

import java.util.Set;

/**
 * Represents a candidate for horizon plugin creation
 *
 * @param fileJar
 *     the io and jar files
 * @param metadata
 *     plugin metadata
 * @param nestedData
 *     the nested jars in this candidate
 */
public record PluginCandidate(FileJar fileJar, CandidateMetadata metadata, NestedData nestedData) {

    public record NestedData(
        Set<PluginCandidate> horizonEntries, Set<FileJar> serverPluginEntries,
        Set<FileJar> libraryEntries
    ) {}
}
