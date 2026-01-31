plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

version = "1.0.0-SNAPSHOT"
val jdkVersion = libs.versions.java.get()

dependencies {
    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    // add horizon api from the core project
    horizon.horizonApi(project(":core"))
}

/*
tasks {
    runServer {
        minecraftVersion("1.21.11") // uses the dev bundle version by default
    }
}
*/

horizon {
    accessTransformerFiles.from(
        file("src/main/resources/widener.at")
    )
    // customRunServerJar = file(...) // allows supplying a custom server jar instead of downloading one
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}
