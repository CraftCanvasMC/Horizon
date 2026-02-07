package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.constants.NEOFORGED_MAVEN_REPO_URL
import io.canvasmc.horizon.util.jij.configureSplitSources
import io.canvasmc.horizon.util.providerSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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
     * Whether to automatically inject Canvas's maven repository for easier Horizon API resolution.
     */
    val injectCanvasRepository: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Whether to fail the build if an AT fails to be applied to the dev bundle sources.
     */
    val failFastOnUnapplicableAT: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Configurations to add the Minecraft server dependency to.
     */
    val addServerDependencyTo: SetProperty<Configuration> = objects.setProperty<Configuration>().convention(
        objects.providerSet(
            project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
            project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
        )
    )

    /**
     * Configurations to add the Horizon API dependency to.
     *
     * The dependency may appear as `unspecified` in the dependency tree. This is expected.
     * It will still be available for the configured configurations.
     */
    val addHorizonApiDependencyTo: SetProperty<Configuration> = objects.setProperty<Configuration>().convention(
        objects.providerSet(
            project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
            project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
        )
    )

    /**
     * Configurations to add included (JiJ) dependencies to.
     */
    val addIncludedDependenciesTo: SetProperty<Configuration> = objects.setProperty<Configuration>().convention(
        objects.providerSet(
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
