plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
}

val jdkVersion = 21

dependencies {
    // minecraft setup
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

horizon {
    // failFastOnUnapplicableAT = false
    // splitPluginSourceSets()
    accessTransformerFiles.from(
        file("src/main/resources/wideners.at"),
        file("src/main/resources/additional_wideners.at"),
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}
