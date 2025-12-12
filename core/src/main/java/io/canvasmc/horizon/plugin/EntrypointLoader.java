package io.canvasmc.horizon.plugin;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.ember.EmberMixinService;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.phase.impl.BuilderPhase;
import io.canvasmc.horizon.plugin.phase.impl.DiscoveryPhase;
import io.canvasmc.horizon.plugin.phase.impl.ValidationPhase;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.ClassTransformer;
import io.canvasmc.horizon.service.MixinContainerHandle;
import io.canvasmc.horizon.transformer.AccessTransformationImpl;
import joptsimple.OptionSet;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class EntrypointLoader {

    public static final Supplier<Throwable> PLUGIN_NOT_FOUND_FROM_MAIN = () -> new IllegalArgumentException("Unable to find plugin from string 'main'");
    public static final Supplier<Throwable> PLUGIN_NOT_FOUND_FROM_NAME = () -> new IllegalArgumentException("Unable to find plugin from string 'name'");

    // Note: we use "?" here because we don't want to load the JavaPlugin ref before we load the game
    //     and only contains mappings for horizon plugins
    public static final Map<String, Object> MAIN2JAVA_PLUGIN = new ConcurrentHashMap<>();
    public static final TaggedLogger LOGGER = Logger.tag("pluginloader");

    private static final List<Phase<?, ?>> PHASES = List.of(
        new DiscoveryPhase(),
        new ValidationPhase(),
        new BuilderPhase()
    );
    public static EntrypointLoader INSTANCE = new EntrypointLoader();
    // used for 'mixin.initfix' to store the current plugin provider
    public static AtomicReference<Object> ACTIVE_PLUGIN_PROVIDER_REF = new AtomicReference<>();
    private final Map<String, HorizonPlugin> containersByConfig = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <I, O> O executePhase(@NonNull Phase<I, O> phase, Object input, LoadContext context)
        throws PhaseException {
        return phase.execute((I) input, context);
    }

    public static HorizonPlugin getPluginFromMain(String main) throws Throwable {
        return Horizon.INSTANCE.getPlugins().stream()
            .filter(pl -> pl.pluginMetadata().main().equalsIgnoreCase(main))
            .findFirst().orElseThrow(PLUGIN_NOT_FOUND_FROM_MAIN);
    }

    public static HorizonPlugin getPluginFromPluginClazz(@NonNull Class<?> main) throws Throwable {
        return getPluginFromMain(main.getName());
    }

    public static HorizonPlugin getPluginFromName(String name) throws Throwable {
        return Horizon.INSTANCE.getPlugins().stream()
            .filter(pl -> pl.pluginMetadata().name().equalsIgnoreCase(name))
            .findFirst().orElseThrow(PLUGIN_NOT_FOUND_FROM_NAME);
    }

    public @NonNull HorizonPlugin @NonNull [] init() {
        OptionSet optionSet = Horizon.INSTANCE.getOptions();
        File pluginsDirectory = (File) optionSet.valueOf("plugins");

        if (!pluginsDirectory.exists()) {
            LOGGER.info("No plugins directory exists, creating one");
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

            @SuppressWarnings("unchecked") final List<HorizonPlugin> fullResult = new LinkedList<>(((List<HorizonPlugin>) result));
            fullResult.add(Horizon.INTERNAL_PLUGIN);

            final HorizonPlugin[] plugins = fullResult.toArray(new HorizonPlugin[0]);
            final StringBuilder builder = new StringBuilder();

            List<HorizonMetadata> metas = new LinkedList<>(Arrays.stream(plugins).map(HorizonPlugin::pluginMetadata).toList());

            builder.append(
                "Found {} plugin(s):\n"
                    .replace("{}", String.valueOf(metas.size()))
            );

            for (HorizonMetadata meta : metas.reversed()) {

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

            LOGGER.info(builder.substring(0, builder.length() - 1));

            return plugins;
        } catch (Throwable thrown) {
            LOGGER.error(thrown, "Plugin loading failed");
            throw new RuntimeException("Failed to load plugins", thrown);
        }
    }

    public void finishPluginLoad(final @NonNull ClassTransformer transformer) {
        final AccessTransformationImpl transformerService = transformer.getService(AccessTransformationImpl.class);
        if (transformerService == null) {
            throw new IllegalStateException("Access transforming impl cannot be null!");
        }

        for (HorizonPlugin plugin : Horizon.INSTANCE.getPlugins()) {
            List<String> wideners = plugin.pluginMetadata().accessWideners();
            if (wideners.isEmpty()) {
                continue;
            }

            try {
                transformerService.getContainer().register(plugin);
            } catch (Throwable thrown) {
                throw new RuntimeException("Failed to configure wideners: " + plugin.pluginMetadata().name(), thrown);
            }
        }

        final EmberMixinService service = (EmberMixinService) MixinService.getService();
        final MixinContainerHandle handle = (MixinContainerHandle) service.getPrimaryContainer();

        for (HorizonPlugin plugin : Horizon.INSTANCE.getPlugins()) {
            Path pluginPath = plugin.file().ioFile().toPath();
            handle.addResource(pluginPath.getFileName().toString(), pluginPath);

            final List<String> mixins = plugin.pluginMetadata().mixins();
            if (mixins != null && !mixins.isEmpty()) {
                for (final String config : mixins) {
                    final HorizonPlugin previous = this.containersByConfig.putIfAbsent(config, plugin);
                    if (previous != null) {
                        LOGGER.warn("Skipping duplicate mixin configuration: {} (in {} and {})", config, previous.identifier(), plugin.identifier());
                        continue;
                    }

                    Mixins.addConfiguration(config);
                }

                LOGGER.debug("Added the mixin configurations: {}", String.join(", ", mixins));
            }
        }

        for (final Config config : Mixins.getConfigs()) {
            final HorizonPlugin container = this.containersByConfig.get(config.getName());
            if (container == null) continue;

            final IMixinConfig mixinConfig = config.getConfig();
            mixinConfig.decorate(FabricUtil.KEY_MOD_ID, container.identifier());
            mixinConfig.decorate(FabricUtil.KEY_COMPATIBILITY, FabricUtil.COMPATIBILITY_LATEST);
        }
    }
}
