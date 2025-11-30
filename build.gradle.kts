import org.gradle.kotlin.dsl.maven
import java.time.LocalDateTime

plugins {
    java
    idea
    id("com.gradleup.shadow") version "9.2.2"
}

val PAPER_MAVEN = "https://repo.papermc.io/repository/maven-public/"
val JDK_VERSION = 21

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenCentral()
        maven(PAPER_MAVEN)
    }

    // configuration that shades and includes as an implementation for the core project
    val include by configurations.creating {
        configurations.implementation.get().extendsFrom(this)
    }

    dependencies {
        // general libraries
        include("com.google.code.gson:gson:2.13.2")
        include("org.yaml:snakeyaml:2.3")
        include("it.unimi.dsi:fastutil:8.5.15")
        include("com.google.guava:guava:33.4.0-jre")
        include("net.sf.jopt-simple:jopt-simple:5.0.4")

        // annotations -- compileOnly
        compileOnly("org.jspecify:jspecify:1.0.0")

        // logger libraries
        implementation("org.tinylog:tinylog-api:2.7.0")
        implementation("org.tinylog:tinylog-impl:2.7.0")

        // asm
        include("org.ow2.asm:asm:9.9")
        include("org.ow2.asm:asm-analysis:9.9")
        include("org.ow2.asm:asm-commons:9.9")
        include("org.ow2.asm:asm-tree:9.9")
        include("org.ow2.asm:asm-util:9.9")

        // mixin libraries
        implementation("net.fabricmc:sponge-mixin:0.16.5+mixin.0.8.7")
        implementation("io.github.llamalad7:mixinextras-common:0.5.0")
        implementation("net.fabricmc:access-widener:2.1.0")

    }

    tasks.shadowJar {
        configurations = listOf(project.configurations.runtimeClasspath.get())

        val basePackage = "horizon.libs"

        // this is realistically a *rough* relocation. this won't relocate
        // everything, though we can't have it relocate everything
        include.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val group = artifact.moduleVersion.id.group
            val rootPackage = group.split(".").take(2).joinToString(".")

            relocate(rootPackage, "$basePackage.$rootPackage")
        }

        // relocate license files
        eachFile {
            if (path.endsWith("LICENSE.txt") || path.endsWith("LICENSE") ||
                path.endsWith("NOTICE.txt") || path.endsWith("NOTICE")) {
                path = "horizon/libs/licenses/$name"
            }
        }

        archiveClassifier.set("")

        // configure manifest
        val version = fetchVersion()
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to project.properties["main-class"],
                    "Implementation-Title" to "Horizon",
                    "Implementation-Version" to version,
                    "Specification-Title" to "Horizon",
                    "Specification-Version" to version,
                    "Specification-Vendor" to "CanvasMC Team",
                    "Brand-Id" to "canvasmc:horizon",
                    // jvm agent
                    "Launcher-Agent-Class" to project.properties["instrumentation"],
                    "Can-Redefine-Classes" to true,
                    "Can-Retransform-Classes" to true
                )
            )
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JDK_VERSION))
        }
    }
}

fun fetchVersion(): Provider<String> {
    // fetch build number from jenkins property, if not present
    // then we can assume it is a local version
    return providers.gradleProperty("buildNumber").orElse(
        providers.environmentVariable("BUILD_NUMBER").orElse(
            "local"
        )
    )
}

tasks.register<Jar>("createPublicationJar") {
    // `horizon-build.{ver}.jar`
    // ver == local ? "local" : build number
    val version = fetchVersion()

    archiveFileName.set(version.map { "horizon-build.$it.jar" })

    from(zipTree(project(":core").tasks.shadowJar.flatMap { it.archiveFile }))

    // copy manifest from shade
    manifest {
        attributes(project(":core").tasks.shadowJar.get().manifest.attributes)
    }

    // include horizon license
    val rootLicense = rootProject.file("LICENSE")
    if (rootLicense.exists()) {
        from(rootLicense) {
            into("META-INF")
            rename { "HORIZON_LICENSE" }
        }
    }

    val coreProject = project(":core")
    val shadowConf = coreProject.configurations.getByName("shadow")

    val librariesInfo = buildString {
        appendLine("Horizon - Shaded Libraries Information")
        appendLine("=========================================")
        appendLine()
        appendLine("Build Version: ${version.get()}")
        appendLine("Build Date: ${LocalDateTime.now()}")
        appendLine()
        appendLine("Shaded Libraries:")
        appendLine("-----------------")

        shadowConf.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val group = artifact.moduleVersion.id.group
            val name = artifact.moduleVersion.id.name
            val artifactVersion = artifact.moduleVersion.id.version
            val rootPackage = group.split(".").take(2).joinToString(".")

            appendLine()
            appendLine("Library: $group:$name:$artifactVersion")
            appendLine("  Original Package: $rootPackage")
            appendLine("  Relocated To: horizon.libs.$rootPackage")
        }

        appendLine()
        appendLine("=========================================")
        appendLine("Note: All libraries have been relocated to the 'horizon.libs' package")
        appendLine("to avoid conflicts with other plugins or server dependencies.")
        appendLine()
        appendLine("Horizon's license: META-INF/HORIZON_LICENSE")
        appendLine("Shaded library licenses: horizon/libs/licenses/")
    }

    val infoFile = temporaryDir.resolve("SHADED_LIBRARIES.txt")
    infoFile.writeText(librariesInfo)

    from(infoFile) {
        into("META-INF")
    }
}
