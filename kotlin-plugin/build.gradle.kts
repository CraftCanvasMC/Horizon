import com.diffplug.gradle.spotless.SpotlessExtension
import net.kyori.blossom.BlossomExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.blossom)
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.jvm)
}

val javaVersion = 17
val weaver by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
    configurations.testImplementation.get().extendsFrom(this)
}

repositories {
    mavenCentral()
    maven("https://maven.canvasmc.io/snapshots")
}

dependencies {
    compileOnly(gradleApi())
    weaver(libs.userdev)
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs = listOf("-Xjvm-default=all", "-Xjdk-release=$javaVersion")
    }
}

val generatedTestSources = layout.buildDirectory.dir("generated/resources/horizon/test")

val copyWeaverForTests = tasks.register<Copy>("copyWeaverForTests") {
    from(weaver.singleFile)
    into(generatedTestSources.map { it.dir("build-data") })
    rename { "userdev.jar" }
}

tasks.named("processTestResources") {
    dependsOn(copyWeaverForTests)
}

sourceSets {
    test {
        resources { srcDir(generatedTestSources) }
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Version" to project.version
        )
    }
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = Charsets.UTF_8.name()
}

sourceSets.all {
    blossom.kotlinSources {
        properties.put("jst_version", providers.gradleProperty("jstVersion"))
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest(embeddedKotlinVersion)
            dependencies {
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.params)
                implementation(libs.junit.platform.launcher)
            }
            targets.configureEach {
                testTask {
                    testLogging {
                        events(TestLogEvent.FAILED)
                        exceptionFormat = TestExceptionFormat.FULL
                    }
                }
            }
        }
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

configurations.all {
    if (name == "compileOnly") {
        return@all
    }
    dependencies.remove(project.dependencies.gradleApi())
}

configurations.runtimeElements {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaVersion)
        attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named("9.0.0"))
    }
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
