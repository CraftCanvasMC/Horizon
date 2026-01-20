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

/*
tasks {
    runServer {
        systemProperty("Horizon.serverJar", "server.jar") // allows us to override the server jar to use for run task
        minecraftVersion("1.21.11") // uses the dev bundle version by default
    }
}
*/

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