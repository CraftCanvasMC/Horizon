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
    String main,
    List<String> mixins,
    List<String> accessWideners,
    boolean loadDatapackEntry
) { // TODO - dependencies?
    public HorizonMetadata {
        // we don't need to validate arguments here, as pre construction they already are
        name = name.toLowerCase();
        // setup option args so they are non-null
        mixins = List.copyOf(mixins == null ? Collections.emptyList() : mixins);
        accessWideners = List.copyOf(accessWideners == null ? Collections.emptyList() : accessWideners);
    }
}
