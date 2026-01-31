plugins {
    `java-library`
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

// configuration that includes as an implementation for the core project and stores fetch data
val include by configurations.creating {
    extendsFrom(configurations.api.get())
    isCanBeConsumed = false
    isCanBeResolved = false
}

configurations {
    implementation {
        extendsFrom(include)
    }
}

val includeResolvable by configurations.creating {
    extendsFrom(include)
    isCanBeConsumed = false
    isCanBeResolved = true
}

val collectIncludedDependencies = tasks.register<CollectDependenciesTask>("collectIncludedDependencies") {
    dependencies.setFrom(configurations.named("includeResolvable"))
    repositoryData.set(
        providers.provider {
            repositories.withType<MavenArtifactRepository>()
                .filter { it.url.scheme.lowercase() in setOf("http", "https") }
                .map { CollectDependenciesTask.RepositoryData(it.name, it.url.toString()) }
                .distinctBy { it.url }
        }
    )
    outputDir.set(layout.buildDirectory.dir("included-deps"))
}

val createPublicationJar = tasks.register<Jar>("createPublicationJar") {
    // `horizon-{ver[-channel]}.{build}.jar`
    // ver == local ? "local" : build number
    val version = project.version

    // configure manifest
    val main = project.properties["mainClass"]
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

    archiveFileName.set("horizon.$version.jar")
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

// remove the default jar and publish the publication one
// this is purely for run-task integration, as we need to have a runnable jar
configurations.runtimeElements {
    outgoing.artifacts.clear()
    outgoing.artifact(createPublicationJar)
}

configurations.apiElements {
    outgoing.artifacts.clear()
    outgoing.artifact(createPublicationJar)
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
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
