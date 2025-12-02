package io.canvasmc.horizon

import io.canvasmc.horizon.extension.HorizonExtension
import io.canvasmc.horizon.tasks.ApplyClassAccessTransformers
import io.canvasmc.horizon.tasks.ApplySourceAccessTransformers
import io.canvasmc.horizon.tasks.MergeAccessTransformers
import io.canvasmc.horizon.util.*
import io.canvasmc.horizon.util.constants.*
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.userdev.internal.setup.UserdevSetupTask
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_CONFIG
import io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Delete
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
        // apply userdev
        target.pluginManager.apply(Plugins.WEAVER_USERDEV_PLUGIN_ID)
        val userdevExt = target.extensions.getByType(PaperweightUserExtension::class)
        userdevExt.injectServerJar.set(false) // dont add the server jar to the configurations as we override it

        val ext = target.extensions.create<HorizonExtension>(HORIZON_NAME, target)

        target.configurations.register(JST_CONFIG) {
            defaultDependencies {
                add(target.dependencies.create("io.canvasmc.jst:jst-cli-bundle:${LibraryVersions.JST}"))
            }
        }

        target.afterEvaluate { setup(ext) }
    }

    private fun Project.setup(ext: HorizonExtension) {
        val mergeAccessTransformersTask = tasks.register<MergeAccessTransformers>("mergeAccessTransformers")
        val applySourceTransformersTask = tasks.register<ApplySourceAccessTransformers>("applyAccessTransformersToSources")
        val applyClassTransformersTask = tasks.register<ApplyClassAccessTransformers>("applyAccessTransformersToClasses")
        val setupTask = tasks.register("horizonSetup")
        val cleanTask = tasks.register<Delete>("cleanHorizonCache")
        val userdevTask = tasks.named<UserdevSetupTask>(USERDEV_SETUP_TASK_NAME)

        // repository for JST
        repositories {
            maven(ext.jstRepo) {
                name = JST_REPO_NAME
                content { onlyForConfigurations(JST_CONFIG) }
            }
        }

        mergeAccessTransformersTask {
            group = INTERNAL_TASK_GROUP
            files.from(ext.accessTransformerFiles)
            outputFile.set(layout.cache.resolve(horizonTaskOutput("merged", "at")))
        }

        applySourceTransformersTask {
            group = INTERNAL_TASK_GROUP
            mappedServerJar.set(userdevTask.flatMap { it.mappedServerJar })
            processedServerJar.set(layout.cache.resolve(horizonTaskOutput("processedServerJar", "jar")))
            atFile.set(mergeAccessTransformersTask.flatMap { it.outputFile })
            ats.jst.from(project.configurations.named(JST_CONFIG))
        }

        applyClassTransformersTask {
            group = INTERNAL_TASK_GROUP
            inputJar.set(applySourceTransformersTask.flatMap { it.processedServerJar })
            outputJar.set(layout.cache.resolve(horizonTaskOutput("transformedServerJar", "jar")))
            atFile.set(mergeAccessTransformersTask.flatMap { it.outputFile })
        }

        setupTask {
            group = TASK_GROUP
            dependsOn(applyClassTransformersTask)
        }

        cleanTask {
            group = TASK_GROUP
            description = "Delete the project-local horizon setup cache."
            delete(layout.cache)
            delete(rootProject.layout.cache.resolve(HORIZON_NAME))
        }

        tasks.named("classes") { dependsOn(setupTask) } // this also attaches the task to the lifecycle

        configurations.register(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG) {
            dependencies.add((dependencyFactory.create(files(applyClassTransformersTask.flatMap { it.outputJar }))))
        }

        configurations.register(TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG) {
            dependencies.add((dependencyFactory.create(files(applyClassTransformersTask.flatMap { it.outputJar }))))
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
}
