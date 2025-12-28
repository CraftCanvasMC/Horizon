package io.canvasmc.horizon

import io.canvasmc.horizon.extension.HorizonExtension
import io.canvasmc.horizon.extension.HorizonUserDependenciesExtension
import io.canvasmc.horizon.tasks.ApplyClassAccessTransforms
import io.canvasmc.horizon.tasks.ApplySourceAccessTransforms
import io.canvasmc.horizon.tasks.MergeAccessTransformers
import io.canvasmc.horizon.util.*
import io.canvasmc.horizon.util.constants.*
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.userdev.internal.setup.UserdevSetupTask
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_CONFIG
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ProjectLayout
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

abstract class Horizon : Plugin<Project> {
    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val dependencyFactory: DependencyFactory

    @get:Inject
    abstract val objects: ObjectFactory

    override fun apply(target: Project) {
        printId<Horizon>(HORIZON_NAME, target.gradle)
        // check for userdev
        target.checkForWeaverUserdev()
        val userdevExt = target.extensions.getByType(PaperweightUserExtension::class)
        userdevExt.injectServerJar.set(false) // dont add the server jar to the configurations as we override it

        val ext = target.extensions.create<HorizonExtension>(HORIZON_NAME, target)

        target.tasks.register<Delete>("cleanHorizonCache") {
            group = HORIZON_NAME
            description = "Delete the project-local horizon setup cache."
            delete(target.rootProject.layout.cache.resolve(HORIZON_NAME))
        }

        target.configurations.register(JST_CONFIG) {
            defaultDependencies {
                add(target.dependencies.create("io.canvasmc.jst:jst-cli-bundle:${LibraryVersions.JST}"))
            }
        }

        val horizonApi = target.configurations.register(HORIZON_API_CONFIG)

        // configurations for JiJ
        val includeMixinPlugin = target.configurations.register(INCLUDE_MIXIN_PLUGIN)
        val includePlugin = target.configurations.register(INCLUDE_PLUGIN)
        val includeLibrary = target.configurations.register(INCLUDE_LIBRARY)

        target.configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).configure {
            extendsFrom(includeMixinPlugin.get(), includePlugin.get(), includeLibrary.get(), horizonApi.get())
        }

        target.dependencies.extensions.create(
            HORIZON_NAME,
            HorizonUserDependenciesExtension::class,
            target.dependencies,
            target.dependencyFactory,
        )

        target.afterEvaluate { setup(ext) }
    }

    private fun Project.setup(ext: HorizonExtension) {
        // ensure people specify a dependency on horizon api
        // checkForHorizonApi()
        val userdevTask = tasks.named<UserdevSetupTask>(USERDEV_SETUP_TASK_NAME)

        repositories {
            // repository for JST
            maven(ext.jstRepo) {
                name = JST_REPO_NAME
                content { onlyForConfigurations(JST_CONFIG) }
            }
            // repository for Horizon API
            if (ext.injectCanvasRepository.get()) {
                maven(CANVAS_MAVEN_REPO_URL) {
                    name = HORIZON_API_REPO_NAME
                    content { onlyForConfigurations(HORIZON_API_CONFIG) }
                }
            }
        }

        val mergeAccessTransformers by tasks.registering<MergeAccessTransformers> {
            files.from(ext.accessTransformerFiles)
            outputFile.set(layout.cache.resolve(horizonTaskOutput("merged", "at")))
        }

        val applySourceAccessTransforms by tasks.registering<ApplySourceAccessTransforms> {
            mappedServerJar.set(userdevTask.flatMap { it.mappedServerJar })
            sourceTransformedMappedServerJar.set(
                layout.cache.resolve(
                    horizonTaskOutput(
                        "sourceTransformedMappedServerJar",
                        "jar"
                    )
                )
            )
            atFile.set(mergeAccessTransformers.flatMap { it.outputFile })
            ats.jst.from(project.configurations.named(JST_CONFIG))
        }

        val applyClassAccessTransforms by tasks.registering<ApplyClassAccessTransforms> {
            inputJar.set(applySourceAccessTransforms.flatMap { it.sourceTransformedMappedServerJar })
            outputJar.set(layout.cache.resolve(horizonTaskOutput("transformedMappedServerJar", "jar")))
            atFile.set(mergeAccessTransformers.flatMap { it.outputFile })
        }

        val horizonSetup by tasks.registering<Task> {
            group = HORIZON_NAME
            dependsOn(applyClassAccessTransforms)
        }

        // JiJ
        tasks.named<Jar>("jar") {
            from(configurations.named(INCLUDE_MIXIN_PLUGIN)) {
                into(EMBEDDED_MIXIN_PLUGIN_JAR_PATH)
            }
            from(configurations.named(INCLUDE_PLUGIN)) {
                into(EMBEDDED_PLUGIN_JAR_PATH)
            }
            from(configurations.named(INCLUDE_LIBRARY)) {
                into(EMBEDDED_LIBRARY_JAR_PATH)
            }
        }

        tasks.named("classes") { dependsOn(horizonSetup) } // this also attaches the task to the lifecycle

        configurations.register(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG) {
            dependencies.add((dependencyFactory.create(files(applyClassAccessTransforms.flatMap { it.outputJar }))))
        }

        configurations.register(TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG) {
            dependencies.add((dependencyFactory.create(files(applyClassAccessTransforms.flatMap { it.outputJar }))))
        }

        // attach sources into original paperweight configurations for compatibility reasons
        configurations.named(MOJANG_MAPPED_SERVER_CONFIG).configure {
            extendsFrom(configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG).get())
        }
        configurations.named(MOJANG_MAPPED_SERVER_RUNTIME_CONFIG).configure {
            extendsFrom(configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG).get())
        }
    }

    // taken from paperweight
    inline fun <reified P> printId(pluginId: String, gradle: Gradle) {
        if (gradle.startParameter.logLevel == LogLevel.QUIET) {
            return
        }
        println("$pluginId v${P::class.java.`package`.implementationVersion} (running on '${System.getProperty("os.name")}')")
    }

    private fun Project.checkForWeaverUserdev() {
        runCatching {
            project.pluginManager.apply(Plugins.WEAVER_USERDEV_PLUGIN_ID)
        }

        val hasUserdev = project.pluginManager.hasPlugin(Plugins.WEAVER_USERDEV_PLUGIN_ID)
        if (!hasUserdev) {
            val message =
                "Unable to find the weaver userdev plugin, which is needed in order for Horizon to work properly, " +
                    "due to Horizon depending on a dev bundle being present and hooking into internal weaver functionality.\n" +
                    "Please apply the weaver userdev plugin in order to resolve this issue and use Horizon."
            throw RuntimeException(message)
        }
    }

    private fun Project.checkForHorizonApi() {
        val hasHorizonApi = runCatching {
            !configurations.getByName(HORIZON_API_CONFIG).isEmpty
        }

        if (hasHorizonApi.isFailure || !hasHorizonApi.getOrThrow()) {
            val message =
                "Unable to resolve the Horizon API dependency, which is required in order to work with mixins.\n" +
                    "Add Horizon API to the `horizonApi` configuration, and ensure there is a repository to resolve it from (the Canvas repository is used by default)."
            throw RuntimeException(message)
        }
    }
}
