plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val jdkVersion = libs.versions.java.get()

dependencies {
    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    // add horizon api from the core project
    horizon.horizonApi(project(":core", configuration = "publicationJar"))
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
}

horizon {
    accessTransformerFiles.from(
        file("src/main/resources/widener.at")
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}