package io.canvasmc.horizon.util.constants

const val HORIZON_NAME = "horizon"
const val INTERNAL_TASK_GROUP = "$HORIZON_NAME internal"
const val CANVAS_MAVEN_RELEASES_REPO_URL = "https://maven.canvasmc.io/releases"
const val JST_REPO_NAME = "${HORIZON_NAME}JstRepository"
const val JST_CONFIG = "${HORIZON_NAME}JstConfig"
const val HORIZON_API_REPO_NAME = "${HORIZON_NAME}HorizonApiRepository"
const val HORIZON_API_CONFIG = "${HORIZON_NAME}HorizonApiConfig"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG = "transformedMojangMappedServer"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG = "transformedMojangMappedServerRuntime"
const val EMBEDDED_MIXIN_PLUGIN_JAR_PATH = "META-INF/jars/horizon"
const val EMBEDDED_PLUGIN_JAR_PATH = "META-INF/jars/plugin"
const val EMBEDDED_LIBRARY_JAR_PATH = "META-INF/jars/libs"
const val INCLUDE_MIXIN_PLUGIN = "includeMixinPlugin"
const val INCLUDE_PLUGIN = "includePlugin"
const val INCLUDE_LIBRARY = "includeLibrary"
const val CACHE_PATH = "caches"
const val HORIZON_API_GROUP = "io.canvasmc.horizon"
const val HORIZON_API_ARTIFACT_ID = "core"
private const val TASK_CACHE = "$HORIZON_NAME/taskCache"

object Paperweight {
    const val USERDEV_SETUP_TASK_NAME = "paperweightUserdevSetup"
    const val MOJANG_MAPPED_SERVER_CONFIG = io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_CONFIG
    const val MOJANG_MAPPED_SERVER_RUNTIME_CONFIG = io.papermc.paperweight.util.constants.MOJANG_MAPPED_SERVER_RUNTIME_CONFIG
}

object Plugins {
    const val WEAVER_USERDEV_PLUGIN_ID = "io.canvasmc.weaver.userdev"
}

fun horizonTaskOutput(name: String, ext: String? = null) = "$TASK_CACHE/$name" + (ext?.let { ".$it" } ?: "")
