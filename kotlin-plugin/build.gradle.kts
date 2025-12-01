import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.kyori.blossom") version "2.2.0"
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.0"
    id("com.diffplug.spotless") version "8.1.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
}

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
    mavenLocal()
    // maven("https://maven.canvasmc.io/snapshots")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("io.canvasmc.weaver.userdev:io.canvasmc.weaver.userdev.gradle.plugin:2.3.11-SNAPSHOT")
}

java {
    withSourcesJar()
}

tasks.withType(JavaCompile::class).configureEach {
    options.release = 17
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("horizon")
}

sourceSets.main {
    blossom {
        kotlinSources {
            properties.put("jst_version", providers.gradleProperty("jstVersion"))
        }
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
            name = "CanvasMC_Snapshots"
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
        maven("https://maven.canvasmc.io/releases") {
            name = "CanvasMC_Releases"
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = "horizon"
            }
        }
    }
}
