package io.canvasmc.horizon.inject.mixin;

import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.logger.Logger;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class MixinConfig implements IMixinConfigPlugin {

    private static final Logger LOGGER = Logger.fork(Horizon.LOGGER, "horizon-mixin");

    private static final String MIXIN_ROOT = "io.canvasmc.horizon.inject.mixin.";
    private static final String DISABLE_PREFIX = "Horizon.disable.mixin.";

    private static final Set<String> DISABLED_PACKAGES = discoverDisabledMixinPackages();
    private static boolean logged = false;

    private static Set<String> discoverDisabledMixinPackages() {
        Properties props = System.getProperties();

        return props.stringPropertyNames()
            .stream()
            .filter(key -> key.startsWith(DISABLE_PREFIX))
            .map(key -> MIXIN_ROOT + key.substring(DISABLE_PREFIX.length()))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("Loading Horizon mixins: {}", mixinPackage);

        if (!logged) {
            logged = true;
            if (!DISABLED_PACKAGES.isEmpty()) {
                LOGGER.info("Disabled mixin packages: {}", DISABLED_PACKAGES);
            }
        }
    }

    @Contract(pure = true)
    @Override
    public @Nullable String getRefMapperConfig() {
        return null; // horizon does not use a refmap
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        for (String disabled : DISABLED_PACKAGES) {
            if (mixinClassName.startsWith(disabled)) {
                LOGGER.debug(
                    "Skipping mixin {} (disabled via -D{})",
                    mixinClassName,
                    DISABLE_PREFIX + disabled.substring(MIXIN_ROOT.length())
                );
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Contract(pure = true)
    @Override
    public @Nullable List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
