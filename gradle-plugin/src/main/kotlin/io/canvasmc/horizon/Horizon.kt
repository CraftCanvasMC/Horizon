package io.canvasmc.horizon

import io.canvasmc.horizon.compatibility.setupRunPaperCompat
import io.canvasmc.horizon.extension.HorizonExtension
import io.canvasmc.horizon.extension.HorizonUserDependenciesExtension
import io.canvasmc.horizon.tasks.ApplyClassAccessTransforms
import io.canvasmc.horizon.tasks.ApplySourceAccessTransforms
import io.canvasmc.horizon.tasks.MergeAccessTransformers
import io.canvasmc.horizon.util.*
import io.canvasmc.horizon.util.constants.*
import io.canvasmc.horizon.util.jij.configureJiJ
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.userdev.internal.setup.UserdevSetupTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

abstract class Horizon : Plugin<Project> {

    @get:Inject
    protected abstract val progressLoggerFactory: ProgressLoggerFactory

    override fun apply(target: Project) {
        printId<Horizon>(HORIZON_NAME, target.gradle)
        val ext = target.extensions.create<HorizonExtension>(HORIZON_NAME, target)
        // check for userdev
        target.checkForWeaverUserdev()
        val userdevExt = target.extensions.getByType(PaperweightUserExtension::class)
        userdevExt.injectServerJar.set(false) // dont add the server jar to the configurations as we override it

        target.tasks.register<Delete>("cleanHorizonCache") {
            group = HORIZON_NAME
            description = "Delete the project-local horizon setup cache."
            delete(target.layout.cache.resolve(HORIZON_NAME))
        }

        target.configurations.register(JST_CONFIG) {
            defaultDependencies {
                add(target.dependencies.create("net.neoforged.jst:jst-cli-bundle:${LibraryVersions.JST}"))
            }
        }

        // user configuration for Horizon API
        val horizonApi = target.configurations.dependencyScope(HORIZON_API_CONFIG)

        // resolvable configuration for Horizon API
        target.configurations.resolvable(HORIZON_API_RESOLVABLE_CONFIG) {
            extendsFrom(horizonApi.get())
        }

        // resolvable non-transitive configuration for Horizon API for use in run tasks
        target.configurations.resolvable(HORIZON_API_SINGLE_RESOLVABLE_CONFIG) {
            extendsFrom(horizonApi.get())
            isTransitive = false
        }

        // configurations for JiJ
        target.configurations.register(INCLUDE_MIXIN_PLUGIN)
        target.configurations.register(INCLUDE_PLUGIN)
        target.configurations.register(INCLUDE_LIBRARY)

        target.configurations.register(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG)
        target.configurations.register(TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG)

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
        val userdevExt = extensions.getByType(PaperweightUserExtension::class)
        val userdevTask = tasks.named<UserdevSetupTask>(Paperweight.USERDEV_SETUP_TASK_NAME)

        // setup run paper compat layer
        if (ext.setupRunPaperCompatibility.get()) {
            plugins.withId(Plugins.RUN_TASK_PAPER_PLUGIN_ID) {
                setupRunPaperCompat(userdevExt, ext, progressLoggerFactory)
            }
        }

        // populate compile classpath
        ext.addServerDependencyTo.get().forEach {
            it.extendsFrom(configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG).get())
        }

        repositories {
            // repository for JST
            maven(ext.jstRepo) {
                name = JST_REPO_NAME
                content { onlyForConfigurations(JST_CONFIG) }
            }
            // repository for Horizon API
            if (ext.injectCanvasRepository.get()) {
                maven(CANVAS_MAVEN_RELEASES_REPO_URL) {
                    name = HORIZON_API_REPO_NAME
                    content { onlyForConfigurations(HORIZON_API_RESOLVABLE_CONFIG, HORIZON_API_SINGLE_RESOLVABLE_CONFIG) }
                }
            }
        }

        // set up horizon api dependency
        ext.addHorizonApiDependencyTo.get().forEach {
            // we want to resolve it from the context of the horizon api configurations so pass only the files
            // do not extend as that would resolve it from the context of compileClasspath which we dont want
            it.dependencies.add(dependencyFactory.create(files(configurations.named(HORIZON_API_RESOLVABLE_CONFIG))))
        }

        // configure JiJ
        configureJiJ(ext)

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
            failFast.set(ext.failFastOnUnapplicableAT)
            atFile.set(mergeAccessTransformers.flatMap { it.outputFile })
            ats.jst.from(configurations.named(JST_CONFIG))
            ats.jstClasspath.from(configurations.named(Paperweight.MOJANG_MAPPED_SERVER_CONFIG))
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

        tasks.named("classes") { dependsOn(horizonSetup) } // this also attaches the task to the lifecycle

        configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG).configure {
            defaultDependencies {
                add((dependencyFactory.create(files(applyClassAccessTransforms.flatMap { it.outputJar }))))
            }
        }

        configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG).configure {
            defaultDependencies {
                add((dependencyFactory.create(files(applyClassAccessTransforms.flatMap { it.outputJar }))))
            }
        }
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
                    "Add Horizon API to the `horizonHorizonApiConfig` configuration, and ensure there is a repository to resolve it from (the Canvas repository is used by default)."
            throw RuntimeException(message)
        }
    }
}
