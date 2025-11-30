package io.canvasmc.horizon.plugin.data;

import java.util.Map;

/**
 * Represents the plugin yaml during the candidate phase
 *
 * @param name    name of the plugin
 * @param version version of the plugin
 * @param rawData the full map data of the yaml file
 */
public record CandidateMetadata(String name, String version, Map<String, Object> rawData) {
}