package io.canvasmc.horizon.util.jij

import io.canvasmc.horizon.extension.HorizonExtension
import io.canvasmc.horizon.util.constants.EMBEDDED_LIBRARY_JAR_PATH
import io.canvasmc.horizon.util.constants.EMBEDDED_MIXIN_PLUGIN_JAR_PATH
import io.canvasmc.horizon.util.constants.EMBEDDED_PLUGIN_JAR_PATH
import io.canvasmc.horizon.util.constants.INCLUDE_LIBRARY
import io.canvasmc.horizon.util.constants.INCLUDE_MIXIN_PLUGIN
import io.canvasmc.horizon.util.constants.INCLUDE_PLUGIN
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import kotlin.collections.forEach

fun Project.configureJiJ(ext: HorizonExtension) {
    ext.addIncludedDependenciesTo.get().forEach {
        it.extendsFrom(
            configurations.named(INCLUDE_MIXIN_PLUGIN).get(),
            configurations.named(INCLUDE_PLUGIN).get(),
            configurations.named(INCLUDE_LIBRARY).get()
        )
    }
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
}

fun Project.configureSplitSources() {
    val javaPlugin = extensions.getByType<JavaPluginExtension>()
    val main = javaPlugin.sourceSets.named("main")

    val plugin = javaPlugin.sourceSets.register("plugin") {
        // call get to avoid IDE sync issues
        compileClasspath += main.get().output + main.get().compileClasspath
        runtimeClasspath += main.get().output + main.get().runtimeClasspath
    }

    val pluginJar = tasks.register<Jar>("pluginJar") {
        archiveClassifier.set("plugin")
        from(plugin.map { it.output })
    }

    tasks.named<Jar>("jar") {
        from(pluginJar.map { it.archiveFile }) {
            into(EMBEDDED_PLUGIN_JAR_PATH)
        }
    }
}
