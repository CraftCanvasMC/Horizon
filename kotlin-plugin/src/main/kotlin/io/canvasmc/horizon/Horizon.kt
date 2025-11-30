package io.canvasmc.horizon

import io.canvasmc.horizon.util.constants.*
import io.papermc.paperweight.userdev.internal.setup.UserdevSetupTask
import io.canvasmc.horizon.util.*
import io.canvasmc.horizon.extension.HorizonExtension
import io.canvasmc.horizon.tasks.SetupEnvironment
import io.papermc.paperweight.userdev.PaperweightUserExtension
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*

abstract class Horizon : Plugin<Project> {
    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val dependencyFactory: DependencyFactory

    @get:Inject
    abstract val objects: ObjectFactory

    override fun apply(target: Project) {
        printId<Horizon>("horizon", target.gradle)

        // apply userdev
        target.pluginManager.apply("io.canvasmc.weaver.userdev")
        // we reset some values so our slate doesnt get polluted
        val userdevExt = target.extensions.getByType(PaperweightUserExtension::class)
        userdevExt.addServerDependencyTo.set(emptyList())
        userdevExt.injectServerJar.set(false) // gotta love weaver for this!

        val ext = target.extensions.create<HorizonExtension>(HORIZON_EXTENSION_NAME, target)

        target.configurations.register(JST_CONFIG) {
            defaultDependencies {
                add(target.dependencies.create("io.canvasmc.jst:jst-cli-bundle:${LibraryVersions.JST}"))
            }
        }

        target.afterEvaluate { setup(ext) }
    }

    private fun Project.setup(ext: HorizonExtension) {
        val setupTask = tasks.register<SetupEnvironment>("setupEnvironment")
        val userdevTask = tasks.named<UserdevSetupTask>(USERDEV_SETUP_TASK_NAME)

        setupTask {
            group = "horizon"
            processedServerJar.set(userdevTask.flatMap { it.processedServerJar })
            transformedServerJar.set(layout.cache.resolve(horizonTaskOutput("transformedServerJar", "jar")))
            atFile.set(ext.accessTransformerFile)
            ats.jst.from(project.configurations.named(JST_CONFIG))
            doLast {
            }
        }

        tasks.named("classes") { dependsOn(setupTask) } // this also attaches it to the lifecycle

        // attach sources
        configurations.register(MOJANG_MAPPED_SERVER_CONFIG) {
            defaultDependencies { add(dependencyFactory.create(files(setupTask.flatMap { it.transformedServerJar }))) }
            extendsFrom(configurations.getByName(io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_CONFIG))
        }
        configurations.register(MOJANG_MAPPED_SERVER_RUNTIME_CONFIG) {
            defaultDependencies { add(dependencyFactory.create(files(setupTask.flatMap { it.transformedServerJar }))) }
            extendsFrom(configurations.getByName(io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG))
        }

        ext.addServerDependencyTo.get().forEach {
            it.extendsFrom(configurations.getByName(MOJANG_MAPPED_SERVER_CONFIG))
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
