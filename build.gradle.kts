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

    dependencies {
        // general libraries
        shadow("com.google.code.gson:gson:2.11.0")
        shadow("org.yaml:snakeyaml:2.3")
        shadow("it.unimi.dsi:fastutil:8.5.15")
        shadow("com.google.guava:guava:33.4.0-jre")
        shadow("net.sf.jopt-simple:jopt-simple:5.0.4")

        // annotations -- compileOnly
        compileOnly("org.jetbrains:annotations:26.0.2")

        // logger libraries
        implementation("org.tinylog:tinylog-api:2.7.0")
        implementation("org.tinylog:tinylog-impl:2.7.0")
    }

    tasks.shadowJar {
        configurations = listOf(project.configurations.runtimeClasspath.get())

        val basePackage = "horizon.libs"
        val shadowConf = project.configurations.getByName("shadow")

        shadowConf.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
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
