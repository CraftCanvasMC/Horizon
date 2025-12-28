import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import net.kyori.blossom.BlossomExtension

plugins {
    id("net.kyori.blossom") version "2.2.0"
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.0"
    id("com.diffplug.spotless") version "8.1.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
}

val USERDEV_VERSION = "2.3.12-SNAPSHOT"

gradlePlugin {
    plugins.register("horizon") {
        id = "io.canvasmc.horizon"
        displayName = "horizon"
        tags.set(listOf("plugins", "mixin", "minecraft", "canvas"))
        implementationClass = "io.canvasmc.horizon.Horizon"
        description = "Gradle plugin for developing plugins using the Horizon framework, allowing for mixin and AT usage"
    }
}

repositories {
    gradlePluginPortal()
    maven("https://maven.canvasmc.io/snapshots")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("io.canvasmc.weaver.userdev:io.canvasmc.weaver.userdev.gradle.plugin:$USERDEV_VERSION")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("horizon")
}

sourceSets.all {
    val blossom = extensions.findByType(BlossomExtension::class.java) ?: return@all

    blossom.kotlinSources {
        properties.put("jst_version", providers.gradleProperty("jstVersion").get())
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs = listOf("-Xjvm-default=all", "-Xjdk-release=17")
    }
}

configurations.all {
    if (name == "compileOnly") {
        return@all
    }
    dependencies.remove(project.dependencies.gradleApi())
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Version" to project.version
        )
    }
}

extensions.configure<SpotlessExtension> {
    val overrides = mapOf(
        "ktlint_standard_no-wildcard-imports" to "disabled",
        "ktlint_standard_filename" to "disabled",
        "ktlint_standard_trailing-comma-on-call-site" to "disabled",
        "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
    )

    val ktlintVer = "1.8.0"

    kotlin {
        ktlint(ktlintVer).editorConfigOverride(overrides)
    }
    kotlinGradle {
        ktlint(ktlintVer).editorConfigOverride(overrides)
    }
}

tasks.register("format") {
    group = "formatting"
    description = "Formats source code according to project style"
    dependsOn(tasks.named("spotlessApply"))
}

configurations.runtimeElements {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named("9.0.0"))
    }
}

publishing {
    repositories {
        maven("https://maven.canvasmc.io/snapshots") {
            name = "snapshots"
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
        maven("https://maven.canvasmc.io/releases") {
            name = "canvasmc"
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
    }
    publications {
        withType(MavenPublication::class).configureEach {
            pom {
                pomConfig()
            }
        }
    }
}

fun MavenPom.pomConfig() {
    val repoPath = "CraftCanvasMC/Horizon"
    val repoUrl = "https://github.com/$repoPath"

    name.set("horizon")
    description.set("Gradle plugin for the CanvasMC Horizon project")
    url.set(repoUrl)
    inceptionYear.set("2025")

    licenses {
        license {
            name.set("MIT")
            url.set("$repoUrl/blob/HEAD/LICENSE")
            distribution.set("repo")
        }
    }

    issueManagement {
        system.set("GitHub")
        url.set("$repoUrl/issues")
    }

    developers {
        developer {
            id.set("CanvasMC")
            name.set("Canvas")
            url.set("https://github.com/CraftCanvasMC")
        }
    }

    scm {
        url.set(repoUrl)
        connection.set("scm:git:$repoUrl.git")
        developerConnection.set("scm:git:git@github.com:$repoPath.git")
    }
}
