package io.canvasmc.horizon.util.constants

import org.gradle.api.Task

const val HORIZON_NAME = "horizon"
const val INTERNAL_TASK_GROUP = "$HORIZON_NAME internal"
const val USERDEV_SETUP_TASK_NAME = "paperweightUserdevSetup"
const val CANVAS_MAVEN_REPO_URL = "https://maven.canvasmc.io/snapshots"
const val CANVAS_MAVEN_RELEASES_REPO_URL = "https://maven.canvasmc.io/releases"
const val JST_REPO_NAME = "${HORIZON_NAME}JstRepository"
const val JST_CONFIG = "${HORIZON_NAME}JstConfig"
const val HORIZON_API_REPO_NAME = "${HORIZON_NAME}HorizonApiRepository"
const val HORIZON_API_CONFIG = "${HORIZON_NAME}HorizonApiConfig"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG = "transformedMojangMappedServer"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG = "transformedMojangMappedServerRuntime"
const val EMBEDDED_MIXIN_PLUGIN_JAR_PATH = "META-INF/jars/${HORIZON_NAME}"
const val EMBEDDED_PLUGIN_JAR_PATH = "META-INF/jars/plugin"
const val EMBEDDED_LIBRARY_JAR_PATH = "META-INF/jars/libs"
const val INCLUDE_MIXIN_PLUGIN = "includeMixinPlugin"
const val INCLUDE_PLUGIN = "includePlugin"
const val INCLUDE_LIBRARY = "includeLibrary"
const val CACHE_PATH = "caches"
private const val TASK_CACHE = "$HORIZON_NAME/taskCache"

object Plugins {
    const val WEAVER_USERDEV_PLUGIN_ID = "io.canvasmc.weaver.userdev"
}

fun Task.horizonTaskOutput(ext: String? = null) = horizonTaskOutput(name, ext)
fun horizonTaskOutput(name: String, ext: String? = null) = "$TASK_CACHE/$name" + (ext?.let { ".$it" } ?: "")
