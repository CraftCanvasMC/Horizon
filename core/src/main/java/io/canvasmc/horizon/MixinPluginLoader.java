package io.canvasmc.horizon;

import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.PluginTree;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.phase.impl.BuilderPhase;
import io.canvasmc.horizon.plugin.phase.impl.DiscoveryPhase;
import io.canvasmc.horizon.plugin.phase.impl.ValidationPhase;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.service.BootstrapMixinService;
import io.canvasmc.horizon.service.MixinContainerHandle;
import io.canvasmc.horizon.service.transform.ClassTransformer;
import io.canvasmc.horizon.transformer.AccessTransformationImpl;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class MixinPluginLoader {

    public static final Supplier<Throwable> PLUGIN_NOT_FOUND_FROM_NAME = () -> new IllegalArgumentException("Unable to find plugin from string 'name'");

    public static final Map<String, Object> MAIN2JAVA_PLUGIN = new ConcurrentHashMap<>();
    public static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "plugin_loader");

    private static final List<Phase<?, ?>> PHASES = List.of(
        new DiscoveryPhase(),
        new ValidationPhase(),
        new BuilderPhase()
    );
    // used for 'mixin.initfix' to store the current plugin provider
    public static AtomicReference<Object> ACTIVE_PLUGIN_PROVIDER_REF = new AtomicReference<>();
    private final Map<String, HorizonPlugin> containersByConfig = new HashMap<>();

    MixinPluginLoader() {
    }

    @SuppressWarnings("unchecked")
    private static <I, O> O executePhase(@NonNull Phase<I, O> phase, Object input, LoadContext context)
        throws PhaseException {
        return phase.execute((I) input, context);
    }

    public static HorizonPlugin getPluginFromName(String name) throws Throwable {
        return HorizonLoader.getInstance().getPlugins().getAll().stream()
            .filter(pl -> pl.pluginMetadata().name().equalsIgnoreCase(name))
            .findFirst().orElseThrow(PLUGIN_NOT_FOUND_FROM_NAME);
    }

    private static @NonNull LoadContext getLoadContext() {
        File pluginsDirectory = HorizonLoader.getInstance().getProperties().pluginsDirectory();
        File cacheDirectory = HorizonLoader.getInstance().getProperties().cacheLocation();
        if (!pluginsDirectory.isDirectory()) {
            throw new IllegalStateException(
                "Plugins folder '" + pluginsDirectory.getPath() + "' is not a directory!"
            );
        }
        if (!cacheDirectory.isDirectory()) {
            throw new IllegalStateException(
                "Cache folder '" + cacheDirectory.getPath() + "' is not a directory!"
            );
        }
        return new LoadContext(pluginsDirectory, cacheDirectory);
    }

    public @NonNull PluginTree init() {
        LoadContext context = getLoadContext();

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
            fullResult.add(HorizonLoader.INTERNAL_PLUGIN);

            PluginTree tree = PluginTree.from(fullResult.toArray(new HorizonPlugin[0]));
            LOGGER.info(tree.format());

            return tree;
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

        for (HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
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

        final BootstrapMixinService service = (BootstrapMixinService) MixinService.getService();
        final MixinContainerHandle handle = (MixinContainerHandle) service.getPrimaryContainer();

        for (HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
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
