package io.canvasmc.horizon.compatibility

import io.canvasmc.horizon.extension.HorizonExtension
import io.canvasmc.horizon.util.constants.HORIZON_API_SINGLE_CONFIG
import io.papermc.paperweight.userdev.PaperweightUserExtension
import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.runpaper.task.RunServer

fun Project.setupRunPaperCompat(userdevExt: PaperweightUserExtension, horizonExt: HorizonExtension, progressLoggerFactory: ProgressLoggerFactory) {
    val horizonApiSingleConfig = configurations.named(HORIZON_API_SINGLE_CONFIG)
    // filter out javadoc and sources jars from the configuration as not to mess with the classpath
    val horizonJar = horizonApiSingleConfig.map { files ->
        files.filter { f -> !f.name.endsWith("-sources.jar") && !f.name.endsWith("-javadoc.jar") }
    }
    tasks.withType<RunServer>().configureEach {
        val userJar = horizonExt.customRunServerJar
        version.convention(userdevExt.minecraftVersion)
        runClasspath.setFrom(horizonJar)
        runClasspath.disallowChanges()
        doFirst {
            if (userJar.isPresent) {
                require(userJar.get().asFile.exists()) {
                    "customRunServerJar was set but path does not exist: ${userJar.get().asFile.absolutePath}"
                }
                systemProperty("Horizon.serverJar", userJar.get().asFile.absolutePath)
                logger.lifecycle("Using user-provided server jar.")
            } else if (version.isPresent) {
                val serverJar = downloadsApiService.get().resolveBuild(
                    progressLoggerFactory,
                    version.get(),
                    build.get(),
                )
                systemProperty("Horizon.serverJar", serverJar.toAbsolutePath())
            } else {
                error("No version was specified for the '$name' task. Don't know what version to download.")
            }
            logger.lifecycle("Starting Horizon...")
        }
    }
}
