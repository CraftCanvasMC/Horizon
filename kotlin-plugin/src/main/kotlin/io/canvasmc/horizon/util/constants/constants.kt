package io.canvasmc.horizon.util.constants

import org.gradle.api.Task

const val USERDEV_SETUP_TASK_NAME = "paperweightUserdevSetup"
const val HORIZON_EXTENSION_NAME = "horizon"
const val JST_CONFIG = "horizonJstConfig"
const val INTERNAL_TASK_GROUP = "horizon internal"
const val TASK_GROUP = "horizon"
const val CACHE_PATH = "caches"
const val CACHE_DIR = "horizon"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_CONFIG = "transformedMojangMappedServer"
const val TRANSFORMED_MOJANG_MAPPED_SERVER_RUNTIME_CONFIG = "transformedMojangMappedServerRuntime"
private const val TASK_CACHE = "$CACHE_DIR/taskCache"

fun Task.horizonTaskOutput(ext: String? = null) = horizonTaskOutput(name, ext)
fun horizonTaskOutput(name: String, ext: String? = null) = "$TASK_CACHE/$name" + (ext?.let { ".$it" } ?: "")
