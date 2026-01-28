package io.canvasmc.horizon.service;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.MixinLaunch;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.service.transform.TransformPhase;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

public class BootstrapMixinService implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker, IMixinServiceBootstrap {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "mixin_service");
    private static final EmberClassLoader.DynamicClassLoader SETUP_CLASSLOADER = new EmberClassLoader.DynamicClassLoader(new URL[0]);

    public static final String SIDE = Constants.SIDE_SERVER;

    private final ReEntranceLock lock;
    private final MixinContainerHandle container;
    private final MixinTransformationImpl mixinTransformer;

    private boolean isInit = true;
    private int totalProcessedDuringInit = 0;

    private static @NonNull String getCanonicalName(@NonNull String name) {
        return name.replace('/', '.');
    }

    private static @NonNull String getInternalName(@NonNull String name) {
        return name.replace('.', '/');
    }

    public BootstrapMixinService() {
        this.lock = new ReEntranceLock(1);
        this.container = new MixinContainerHandle(getName());
        this.mixinTransformer = HorizonLoader.getInstance().getLaunchService().getTransformer().getService(MixinTransformationImpl.class);
        if (mixinTransformer == null) {
            throw new IllegalStateException("Mixin transformation service not available?");
        }
    }

    public static void loadToInit(@NonNull URL url) {
        SETUP_CLASSLOADER.addURL(url);
    }

    @ApiStatus.Internal
    public void markOutOfInit() {
        isInit = false;
        try {
            SETUP_CLASSLOADER.close();
            // log if we have paper/spigot plugin targets
            if (totalProcessedDuringInit > 0) {
                LOGGER.info("Closed setup classloader, loaded ({}) Paper/Spigot plugin targets", totalProcessedDuringInit);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to close setup classloader", e);
        }
    }

    @Override
    public String getName() {
        return "Horizon";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void prepare() {
        LOGGER.debug("Running prepare for mixin service");
        // we don't do anything for preparing
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(final IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            mixinTransformer.offer((IMixinTransformerFactory) internal);
        }
    }

    @Override
    public void init() {
        LOGGER.debug("Running init for mixin service");
        // we don't do anything for init
    }

    @Override
    public void beginPhase() {
    }

    @Override
    public void checkEnv(final @NonNull Object bootSource) {
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
        final EmberClassLoader loader = HorizonLoader.getInstance().getLaunchService().getClassLoader();
        return loader.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return SIDE;
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
    public @NonNull ILogger getLogger(final @NonNull String name) {
        return HorizonMixinLogger.get(name);
    }

    @Override
    public @NonNull URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public @NonNull Class<?> findClass(final @NonNull String name) throws ClassNotFoundException {
        return Class.forName(name, true, HorizonLoader.getInstance().getLaunchService().getClassLoader());
    }

    @Override
    public @NonNull Class<?> findClass(final @NonNull String name, final boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, HorizonLoader.getInstance().getLaunchService().getClassLoader());
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

        final MixinLaunch launchService = HorizonLoader.getInstance().getLaunchService();
        final EmberClassLoader loader = launchService.getClassLoader();

        final String canonicalName = getCanonicalName(name);
        final String internalName = getInternalName(name);

        EmberClassLoader.@Nullable ClassData entry = loader.classData(canonicalName, TransformPhase.MIXIN);

        if (entry == null) {
            if (!isInit) throw new ClassNotFoundException(canonicalName);
            // we are in init, lets try and see if we can load this shit anyway
            LOGGER.debug("Hidden class found, '{}', attempting to traverse Spigot/Paper plugin files for class", name);
            // for (final URL url : BACKUP_CLASSLOADER.getURLs()) {
            //     LOGGER.info("Using URL {}", url.toString());
            // }
            final String resourceName = name.replace('.', '/').concat(".class");

            URL url = SETUP_CLASSLOADER.findResource(resourceName);
            if (url == null) {
                throw new ClassNotFoundException(canonicalName);
            }
            entry = loader.getClassData(url, resourceName);

            if (entry != null) {
                LOGGER.debug("Successfully loaded '{}' from backup classloader", name);
                totalProcessedDuringInit++;
            }
            else throw new ClassNotFoundException(canonicalName);
        }

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
        final EmberClassLoader loader = HorizonLoader.getInstance().getLaunchService().getClassLoader();
        return loader.hasClass(name);
    }

    @Override
    public String getClassRestrictions(final @NonNull String name) {
        // no restrictions
        return "";
    }

    @Override
    public String getServiceClassName() {
        return "io.canvasmc.horizon.service.BootstrapMixinService";
    }

    @Override
    public void bootstrap() {
        LOGGER.debug("Running bootstrap for bootstrap mixin service");
        // we don't do anything for bootstrap
    }
}
