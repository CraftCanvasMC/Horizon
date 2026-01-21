plugins {
    id("io.canvasmc.weaver.userdev")
    id("publishing-conventions")
}

version = project.version.toString() + "." + fetchBuild().get()

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"
val jdkVersion = libs.versions.java.get()

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
}

fun fetchBuild(): Provider<String> {
    return providers.gradleProperty("buildNumber")
        .orElse(providers.environmentVariable("BUILD_NUMBER"))
        .orElse("local")
}

// setup custom publishing
val publicationJar = configurations.consumable("publicationJar") {
    extendsFrom(configurations.named("includeResolvable").get())
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
publicationComponent.addVariantsFromConfiguration(configurations.named("publicationJar").get()) {}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["publicationComponent"])
        }
    }
}
