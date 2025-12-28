import java.time.LocalDateTime

plugins {
    id("com.gradleup.shadow")
    id("io.canvasmc.weaver.userdev")
}

val PAPER_MAVEN_PUBLIC_URL = "https://repo.papermc.io/repository/maven-public/"
val JDK_VERSION = 21

// configuration that shades and includes as an implementation for the core project
val include by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

repositories {
    mavenCentral()
    maven(PAPER_MAVEN_PUBLIC_URL)
}

dependencies {
    // general libraries
    include(libs.gson)
    include(libs.snakeyaml)
    include(libs.guava)
    include(libs.jackson)

    // annotations -- compileOnly
    compileOnly(libs.jspecify)

    // logger libraries
    implementation(libs.bundles.tinylog)

    // asm
    include(libs.bundles.asm)

    // mixin libraries
    implementation(libs.bundles.mixin)

    // paperclip patching
    include(libs.jbsdiff)

    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)
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
            path.endsWith("NOTICE.txt") || path.endsWith("NOTICE")
        ) {
            path = "horizon/libs/licenses/$name"
        }
    }

    archiveClassifier.set("")
    mergeServiceFiles()

    // configure manifest
    val version = fetchVersion()
    val main = project.properties["main-class"]
    val launchAgent = project.properties["instrumentation"]
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "$main",
                "Implementation-Title" to "Horizon",
                "Implementation-Version" to version,
                "Specification-Title" to "Horizon",
                "Specification-Version" to version,
                "Specification-Vendor" to "CanvasMC Team",
                "Brand-Id" to "canvasmc:horizon",
                // jvm agent
                "Launcher-Agent-Class" to "$launchAgent",
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

extensions.configure<PublishingExtension> {
    repositories {
        maven("https://maven.canvasmc.io/releases") {
            name = "canvasmc"
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
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

    from(zipTree(tasks.shadowJar.flatMap { it.archiveFile }))

    // copy manifest from shade
    manifest {
        attributes(tasks.shadowJar.get().manifest.attributes)
    }

    // include horizon license
    val rootLicense = rootProject.file("LICENSE")
    if (rootLicense.exists()) {
        from(rootLicense) {
            into("META-INF")
            rename { "HORIZON_LICENSE" }
        }
    }

    val shadowConf = configurations.getByName("shadow")

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