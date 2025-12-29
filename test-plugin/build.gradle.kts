plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
}

val jdkVersion = 21

dependencies {
    // libraries required cuz we cant exactly access shaded core
    implementation(libs.bundles.mixin)
    implementation(libs.bundles.tinylog)

    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    // horizon api
    add("horizonHorizonApiConfig", projects.core)
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