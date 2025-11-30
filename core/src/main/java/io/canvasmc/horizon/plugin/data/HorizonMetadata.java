package io.canvasmc.horizon.plugin.data;

import java.util.Collections;
import java.util.List;

/**
 * Represents the plugin yaml in the final phase and full lifecycle of the Horizon plugin
 *
 * @param name              the name of the plugin
 * @param version           the version of the plugin
 * @param apiVersion        the minimum api version the plugin supports
 * @param mixins            the mixin entry names for the plugin
 * @param accessWideners    the widener entry names for the plugin
 * @param loadDatapackEntry if the plugin should be registered as a datapack entry
 */
public record HorizonMetadata(
        String name,
        String version,
        String apiVersion, // TODO - check this when we parse the paperclip version
        List<String> mixins,
        List<String> accessWideners,
        boolean loadDatapackEntry
) {
    public HorizonMetadata {
        // preconditions, required fields
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (version == null) throw new IllegalArgumentException("version cannot be null");
        if (apiVersion == null) throw new IllegalArgumentException("api-version cannot be null");

        // setup option args
        mixins = List.copyOf(mixins == null ? Collections.emptyList() : mixins);
        accessWideners = List.copyOf(accessWideners == null ? Collections.emptyList() : accessWideners);
    }
}
