package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.constants.CANVAS_MAVEN_PUBLIC_REPO_URL
import io.canvasmc.horizon.util.constants.NEOFORGED_MAVEN_REPO_URL
import io.canvasmc.horizon.util.jij.configureSplitSources
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

abstract class HorizonExtension @Inject constructor(
    objects: ObjectFactory,
    private val project: Project
) {
    /**
     * Access transformer files to apply to the server jar.
     */
    abstract val accessTransformerFiles: ConfigurableFileCollection

    /**
     * The repository from which JST should be resolved.
     */
    val jstRepo: Property<String> = objects.property<String>().convention(NEOFORGED_MAVEN_REPO_URL)

    /**
     * The repository from which Horizon API should be resolved.
     */
    val horizonApiRepo: Property<String> = objects.property<String>().convention(CANVAS_MAVEN_PUBLIC_REPO_URL)

    /**
     * Whether to validate AT entries.
     * Will fail the build if any entry doesn't pass the validation.
     */
    val validateATs: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Configuration providers to add the Minecraft server dependency to.
     */
    val addServerDependencyTo =
        objects.setProperty<NamedDomainObjectProvider<out Configuration>>().convention(
            setOf(
                project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
                project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME),
            )
        )

    /**
     * Configuration providers to add the Horizon API dependency to.
     *
     * The dependency may appear as `unspecified` in the dependency tree. This is expected as it's added as a FileCollection.
     * It will still be available for the configured configurations.
     */
    val addHorizonApiDependencyTo =
        objects.setProperty<NamedDomainObjectProvider<out Configuration>>().convention(
            setOf(
                project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
                project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
            )
        )

    /**
     * Configuration providers to add provided runtime plugin dependencies to.
     *
     * These dependencies are available on the compile classpath and in the dev server,
     * but are not embedded into the produced plugin jar.
     */
    val addRuntimePluginTo =
        objects.setProperty<NamedDomainObjectProvider<out Configuration>>().convention(
            setOf(
                project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
                project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
            )
        )

    /**
     * Configuration providers to add included (JiJ) dependencies to.
     */
    val addIncludedDependenciesTo =
        objects.setProperty<NamedDomainObjectProvider<out Configuration>>().convention(
            setOf(
                project.configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME),
                project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
            )
        )

    /**
     * Whether to set up the run-paper compatibility layer.
     */
    val setupRunPaperCompatibility: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * A custom server jar override for run-paper. Allows you to use your own jar as the server jar.
     */
    abstract val customRunServerJar: RegularFileProperty

    /**
     * Splits source sets for Horizon specific plugin code and a normal paper plugin code.
     * The main source set is used for Horizon code and cannot access the plugin source set,
     * and the plugin source set is used for paper plugin code that should be JiJ'd into the final jar,
     * and is able to access classes from the main source set.
     */
    fun splitPluginSourceSets() {
        project.plugins.withType(JavaPlugin::class.java) {
            project.configureSplitSources()
        }
    }
}
