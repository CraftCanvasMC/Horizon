package io.canvasmc.horizon.util.constants

import org.gradle.api.Task

const val CANVAS_MAVEN_REPO_URL = "https://maven.canvasmc.io/snapshots"
const val JST_REPO_NAME = "horizonJstRepository"
const val USERDEV_SETUP_TASK_NAME = "paperweightUserdevSetup"
const val HORIZON_NAME = "horizon"
const val JST_CONFIG = "horizonJstConfig"
const val INTERNAL_TASK_GROUP = "horizon internal"
const val TASK_GROUP = "horizon"
const val CACHE_PATH = "caches"
const val CACHE_DIR = "horizon"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG = "transformedMojangMappedServer"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG = "transformedMojangMappedServerRuntime"
const val EMBEDDED_JAR_PATH = "META-INF/jars"
const val PLUGIN_API = "pluginApi"
const val PLUGIN_IMPLEMENTATION = "pluginImplementation"
const val PLUGIN_COMPILE_ONLY = "pluginCompileOnly"
const val PLUGIN_RUNTIME_ONLY = "pluginRuntimeOnly"
private const val TASK_CACHE = "$CACHE_DIR/taskCache"

object Plugins {
    const val WEAVER_USERDEV_PLUGIN_ID = "io.canvasmc.weaver.userdev"
}

fun Task.horizonTaskOutput(ext: String? = null) = horizonTaskOutput(name, ext)
fun horizonTaskOutput(name: String, ext: String? = null) = "$TASK_CACHE/$name" + (ext?.let { ".$it" } ?: "")
