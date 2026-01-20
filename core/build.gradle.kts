import java.security.MessageDigest

plugins {
    id("io.canvasmc.weaver.userdev")
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"
val jdkVersion = libs.versions.java.get()

// configuration that includes as an implementation for the core project and stores fetch data
val include by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
    isTransitive = true // ensure transitive dependencies are included
    isCanBeConsumed = false
    isCanBeResolved = false
}

include.extendsFrom(configurations.api.get())

val includeResolvable by configurations.creating {
    extendsFrom(include)
    isCanBeConsumed = false
    isCanBeResolved = true
}

repositories {
    mavenCentral()
    maven {
        name = "Paper"
        url = uri(paperMavenPublicUrl)
    }
}

dependencies {
    // general libraries - packaged in minecraft
    include(libs.gson)
    include(libs.snakeyaml)
    include(libs.guava)

    // included for plugin dev
    api(libs.jackson)
    api(libs.bundles.asm)
    api(libs.bundles.mixin)

    // for paperclip impl
    include(libs.jbsdiff)

    // annotations -- compileOnly
    compileOnly(libs.jspecify)

    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
    withSourcesJar()
    withJavadocJar()
}

fun fetchVersion(): Provider<String> {
    val numberProvider =
        providers.gradleProperty("buildNumber")
            .orElse(providers.environmentVariable("BUILD_NUMBER"))
            .orElse("local")

    val channel = rootProject.version.toString()
    return numberProvider.map { "$channel.$it" }
}

tasks.register<Jar>("createPublicationJar") {
    // `horizon-build.{ver}.jar`
    // ver == local ? "local" : build number
    val version = fetchVersion()

    // configure manifest
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
                "Premain-Class" to "$launchAgent",
                "Can-Redefine-Classes" to true,
                "Can-Retransform-Classes" to true
            )
        )
    }

    archiveFileName.set(version.map { "horizon.$it.jar" })
    from(tasks.named<Jar>("jar").map { zipTree(it.archiveFile) })

    from(collectIncludedDependencies.flatMap { it.outputDir }) {
        include("*.context")
        into("META-INF/")
    }

    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))

    // include horizon license
    val rootLicense = rootProject.file("LICENSE")
    if (rootLicense.exists()) {
        from(rootLicense) {
            into("META-INF")
            rename { "HORIZON_LICENSE" }
        }
    }
}

val collectIncludedDependencies = tasks.register<CollectDependenciesTask>("collectIncludedDependencies") {
    artifactFiles.from(includeResolvable)

    resolvedArtifactsData.set(project.provider {
        includeResolvable.resolvedConfiguration.resolvedArtifacts.map { artifact ->
            CollectDependenciesTask.ArtifactData(
                group = artifact.moduleVersion.id.group,
                name = artifact.moduleVersion.id.name,
                version = artifact.moduleVersion.id.version,
                fileName = artifact.file.name,
                filePath = artifact.file.absolutePath
            )
        }
    })

    repositoryData.set(project.provider {
        project.repositories.mapNotNull { repo ->
            when (repo) {
                is MavenArtifactRepository -> CollectDependenciesTask.RepositoryData(
                    name = repo.name,
                    url = repo.url.toString()
                )
                else -> null
            }
        }
    })

    outputDir.set(layout.buildDirectory.dir("included-deps"))
}

// setup custom publishing
val publicationJar = configurations.consumable("publicationJar") {
    extendsFrom(includeResolvable)
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
    outgoing.artifact(tasks.named<Jar>("createPublicationJar").flatMap { it.archiveFile })
    outgoing.artifact(tasks.named<Jar>("javadocJar").flatMap { it.archiveFile })
    outgoing.artifact(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
}

val publicationComponent = publishing.softwareComponentFactory.adhoc("publicationComponent")
    components.add(publicationComponent)
    publicationComponent.addVariantsFromConfiguration(publicationJar) {}

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
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["publicationComponent"])
        }
    }
}

abstract class CollectDependenciesTask : DefaultTask() {
    @get:InputFiles
    @get:Classpath
    abstract val artifactFiles: ConfigurableFileCollection

    @get:Input
    abstract val resolvedArtifactsData: ListProperty<ArtifactData>

    @get:Input
    abstract val repositoryData: ListProperty<RepositoryData>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    data class ArtifactData(
        val group: String,
        val name: String,
        val version: String,
        val fileName: String,
        val filePath: String
    ) : java.io.Serializable

    data class RepositoryData(
        val name: String,
        val url: String
    ) : java.io.Serializable

    @TaskAction
    fun collect() {
        val destDir = outputDir.get().asFile
        destDir.mkdirs()

        val artifacts = resolvedArtifactsData.get()
        val repositories = repositoryData.get()
        val filesByName = artifactFiles.files.associateBy { it.name }

        val metadataLines = mutableListOf<String>()

        artifacts.forEach { data ->
            val file = filesByName[data.fileName]
            if (file != null && file.exists()) {
                val sha256 = file.sha256()

                val group = data.group.replace('.', '/')
                val mavenPath = "$group/${data.name}/${data.version}/${data.fileName}"

                metadataLines.add("${data.group}:${data.name}:${data.version}\t$mavenPath\t$sha256")
            }
        }

        val artifactsFile = destDir.resolve("artifacts.context")
        artifactsFile.writeText(metadataLines.joinToString("\n"))

        metadataLines.clear()
        repositories.forEach { repo ->
            metadataLines.add("${repo.name}\t${repo.url}")
        }

        val repoFile = destDir.resolve("repositories.context")
        repoFile.writeText(metadataLines.joinToString("\n"))
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}