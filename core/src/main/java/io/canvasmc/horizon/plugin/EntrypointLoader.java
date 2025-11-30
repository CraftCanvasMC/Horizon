package io.canvasmc.horizon.plugin;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.phase.impl.BuilderPhase;
import io.canvasmc.horizon.plugin.phase.impl.DiscoveryPhase;
import io.canvasmc.horizon.plugin.phase.impl.ValidationPhase;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import joptsimple.OptionSet;
import org.jspecify.annotations.NonNull;
import org.tinylog.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class EntrypointLoader {

    private static final List<Phase<?, ?>> PHASES = List.of(
        new DiscoveryPhase(),
        new ValidationPhase(),
        new BuilderPhase()
    );

    public static @NonNull HorizonPlugin @NonNull [] init() {
        OptionSet optionSet = Horizon.INSTANCE.getOptions();
        File pluginsDirectory = (File) optionSet.valueOf("plugins");

        if (!pluginsDirectory.exists()) {
            Logger.info("No plugins directory exists, creating one");
            pluginsDirectory.mkdirs();
        }

        if (!pluginsDirectory.isDirectory()) {
            throw new IllegalStateException(
                "Plugins folder '" + pluginsDirectory.getPath() + "' is not a directory!"
            );
        }

        LoadContext context = new LoadContext(pluginsDirectory);

        try {
            Object result = null;

            for (Phase<?, ?> phase : PHASES) {
                try {
                    result = executePhase(phase, result, context);
                } catch (Throwable e) {
                    throw new RuntimeException("Phase '" + phase.getName() + "' failed due to an unexpected exception", e);
                }
            }

            if (result == null) {
                throw new IllegalStateException("Phases returned null value?");
            }

            @SuppressWarnings("unchecked")
            HorizonPlugin[] plugins = ((List<HorizonPlugin>) result).toArray(new HorizonPlugin[0]);
            StringBuilder builder = new StringBuilder();

            List<HorizonMetadata> metas = Arrays.stream(plugins).map(HorizonPlugin::pluginMetadata).toList();

            builder.append(
                "Found {} plugin(s):\n"
                    .replace("{}", String.valueOf(metas.size()))
            );

            for (HorizonMetadata meta : metas) {

                builder.append("\t- ")
                    .append(meta.name())
                    .append(" ")
                    .append(meta.version())
                    .append("\n");

                List<String> mixins = meta.mixins();
                List<String> wideners = meta.accessWideners();

                int totalChildren = mixins.size() + wideners.size();
                int index = 0;

                for (String mixin : mixins) {
                    index++;
                    boolean last = index == totalChildren;
                    builder.append(last ? "\t   \\-- " : "\t   |-- ")
                        .append(mixin)
                        .append("\n");
                }

                for (String widener : wideners) {
                    index++;
                    boolean last = index == totalChildren;
                    builder.append(last ? "\t   \\-- " : "\t   |-- ")
                        .append(widener)
                        .append("\n");
                }
            }

            Logger.info(builder.substring(0, builder.length() - 1));

            return plugins;
        } catch (Throwable e) {
            Logger.error(e, "Plugin loading failed");
            throw new RuntimeException("Failed to load plugins", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> O executePhase(@NonNull Phase<I, O> phase, Object input, LoadContext context)
        throws PhaseException {
        return phase.execute((I) input, context);
    }
}