package io.canvasmc.horizon.ember;

import io.canvasmc.horizon.service.*;
import io.canvasmc.horizon.service.transform.ClassTransformer;
import io.canvasmc.horizon.service.transform.TransformPhase;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Provides the mixin service for Ember.
 *
 * @author vectrix
 * @since 1.0.0
 */
// TODO - rewrite
public final class EmberMixinService implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
    private final ReEntranceLock lock;
    private final MixinContainerHandle container;

    /**
     * Creates a new mixin service.
     *
     * @since 1.0.0
     */
    public EmberMixinService() {
        this.lock = new ReEntranceLock(1);
        this.container = new MixinContainerHandle("Horizon");
    }

    @Override
    public @NonNull String getName() {
        return "Horizon";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void prepare() {
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(final IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            final MixinTransformationImpl transformer = MixinLaunch.getInstance().getTransformer().getService(MixinTransformationImpl.class);
            if (transformer == null) return;

            transformer.offer((IMixinTransformerFactory) internal);
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void beginPhase() {
    }

    @Override
    public void checkEnv(final @NonNull Object bootSource) {
    }

    @Override
    public String getSideName() {
        return Constants.SIDE_SERVER;
    }

    @Override
    public @NonNull ILogger getLogger(final @NonNull String name) {
        return HorizonMixinLogger.get(name);
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return this.container;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public InputStream getResourceAsStream(final @NonNull String name) {
        final EmberClassLoader loader = MixinLaunch.getInstance().getClassLoader();
        return loader.getResourceAsStream(name);
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_8;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_22;
    }

    @Override
    public @NonNull URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public @NonNull Class<?> findClass(final @NonNull String name) throws ClassNotFoundException {
        return Class.forName(name, true, MixinLaunch.getInstance().getClassLoader());
    }

    @Override
    public @NonNull Class<?> findClass(final @NonNull String name, final boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, MixinLaunch.getInstance().getClassLoader());
    }

    @Override
    public @NonNull Class<?> findAgentClass(final @NonNull String name, final boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, MixinLaunch.class.getClassLoader());
    }

    @Override
    public @NonNull ClassNode getClassNode(final @NonNull String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true);
    }

    @Override
    public @NonNull ClassNode getClassNode(final @NonNull String name, final boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, runTransformers, 0);
    }

    @Override
    public @NonNull ClassNode getClassNode(final @NonNull String name, final boolean runTransformers, final int readerFlags) throws ClassNotFoundException, IOException {
        if (!runTransformers) throw new IllegalStateException("ClassNodes must always be provided transformed!");

        final MixinLaunch launch = MixinLaunch.getInstance();
        final EmberClassLoader loader = launch.getClassLoader();
        final ClassTransformer transformer = launch.getTransformer();

        final MixinTransformationImpl mixinTransformer = transformer.getService(MixinTransformationImpl.class);
        if (mixinTransformer == null) throw new ClassNotFoundException("Mixin transformer is not available!");

        final String canonicalName = name.replace('/', '.');
        final String internalName = name.replace('.', '/');

        final EmberClassLoader.@Nullable ClassData entry = loader.classData(canonicalName, TransformPhase.MIXIN);
        if (entry == null) throw new ClassNotFoundException(canonicalName);

        return mixinTransformer.classNode(canonicalName, internalName, entry.data(), readerFlags);
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(final @NonNull String name) {
    }

    @Override
    public void registerInvalidClass(final @NonNull String name) {
    }

    @Override
    public boolean isClassLoaded(final @NonNull String name) {
        final EmberClassLoader loader = MixinLaunch.getInstance().getClassLoader();
        return loader.hasClass(name);
    }

    @Override
    public String getClassRestrictions(final @NonNull String name) {
        return "";
    }
}
