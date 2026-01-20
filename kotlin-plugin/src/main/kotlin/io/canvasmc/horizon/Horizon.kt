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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.*
import xyz.jpenilla.runpaper.task.RunServer
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

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
                add(target.dependencies.create("io.papermc.jst:jst-cli-bundle:${LibraryVersions.JST}"))
            }
        }

        // configuration for Horizon API for compile-time
        target.configurations.register(HORIZON_API_CONFIG)

        // configuration for Horizon API for run tasks
        target.configurations.register(HORIZON_API_SINGLE_CONFIG) {
            isTransitive = false
        }

        // configurations for JiJ
        target.configurations.register(INCLUDE_MIXIN_PLUGIN)
        target.configurations.register(INCLUDE_PLUGIN)
        target.configurations.register(INCLUDE_LIBRARY)

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
                setupRunPaperCompat(userdevExt, ext)
            }
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
                    content { includeModule(HORIZON_API_GROUP, HORIZON_API_ARTIFACT_ID) }
                }
            }
        }

        // set up horizon api dependency
        ext.addHorizonApiDependencyTo.get().forEach {
            it.extendsFrom(configurations.named(HORIZON_API_CONFIG).get())
        }

        // add the JiJ dependencies to appropriate configurations
        ext.addIncludedDependenciesTo.get().forEach {
            it.extendsFrom(
                configurations.named(INCLUDE_MIXIN_PLUGIN).get(),
                configurations.named(INCLUDE_PLUGIN).get(),
                configurations.named(INCLUDE_LIBRARY).get()
            )
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
            failFast.set(ext.failFastOnUnapplicableAT)
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
        configurations.named(Paperweight.MOJANG_MAPPED_SERVER_CONFIG).configure {
            extendsFrom(configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG).get())
        }
        configurations.named(Paperweight.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG).configure {
            extendsFrom(configurations.named(TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG).get())
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

    private fun Project.setupRunPaperCompat(userdevExt: PaperweightUserExtension, horizonExt: HorizonExtension) {
        val horizonApiSingleConfig = configurations.named(HORIZON_API_SINGLE_CONFIG)
        // filter out javadoc and sources jars from the configuration as not to mess with the classpath
        val horizonJar = horizonApiSingleConfig.map { files ->
            files.filter { f -> !f.name.endsWith("-sources.jar") && !f.name.endsWith("-javadoc.jar") }
        }
        tasks.withType<RunServer>().configureEach {
            val offline = offlineMode()
            val userJar = horizonExt.customRunServerJar
            version.convention(userdevExt.minecraftVersion)
            runClasspath.from(horizonJar).disallowChanges()
            doFirst {
                if (userJar.isPresent && userJar.get().asFile.exists()) {
                    systemProperty("Horizon.serverJar", userJar.path.toAbsolutePath())
                    logger.lifecycle("Using user-provider server jar.")
                } else if (offline) {
                    logger.lifecycle("Offline mode is enabled. Not downloading a server jar for the '$name' task.")
                } else if (!version.isPresent) {
                    error("No version was specified for the '$name' task. Don't know what version to download.")
                } else {
                    // download the server jar ourselves
                    val serverJar = downloadsApiService.get().resolveBuild(
                        progressLoggerFactory,
                        version.get(),
                        build.get(),
                    )
                    systemProperty("Horizon.serverJar", serverJar.toAbsolutePath())
                }
                logger.lifecycle("Starting Horizon...")
            }
        }
    }
}
