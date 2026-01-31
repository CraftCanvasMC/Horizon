package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.constants.EMBEDDED_PLUGIN_JAR_PATH
import io.canvasmc.horizon.util.providerSet
import io.papermc.paperweight.util.constants.PAPER_MAVEN_REPO_URL
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
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
    val jstRepo: Property<String> = objects.property<String>().convention(PAPER_MAVEN_REPO_URL)

    /**
     * Whether to automatically inject Canvas's maven repository for easier Horizon API resolution.
     */
    val injectCanvasRepository: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Whether to fail the build if an AT fails to be applied to the dev bundle sources.
     */
    val failFastOnUnapplicableAT: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Configurations to add the Horizon API dependency to.
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
        project.pluginManager.apply(JavaPlugin::class.java)
        val javaPlugin = project.extensions.getByType<JavaPluginExtension>()
        val main = javaPlugin.sourceSets.named("main")

        val plugin = javaPlugin.sourceSets.register("plugin") {
            java.srcDir("src/plugin/java")
            resources.srcDir("src/plugin/resources")

            // call get to avoid IDE sync issues
            compileClasspath += main.get().output + main.get().compileClasspath
            runtimeClasspath += main.get().output + main.get().runtimeClasspath
        }

        // to avoid issues with gradle thinking there's a duplicate when in fact there isn't
        project.tasks.named<Copy>("processPluginResources") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        val pluginJar = project.tasks.register<Jar>("pluginJar") {
            archiveClassifier.set("plugin")
            from(plugin.map { it.output })
        }

        project.tasks.named<Jar>("jar") {
            from(pluginJar.map { it.archiveFile }) {
                into(EMBEDDED_PLUGIN_JAR_PATH)
            }
        }
    }
}
