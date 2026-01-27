package io.canvasmc.horizon.plugin.data;

import io.canvasmc.horizon.util.tree.ObjectTree;

/**
 * Represents the plugin yaml during the candidate phase
 *
 * @param name
 *     name of the plugin
 * @param version
 *     version of the plugin
 * @param rawData
 *     the {@link ObjectTree} implementation of the yaml configuration
 */
public record CandidateMetadata(String name, String version, ObjectTree rawData) {}