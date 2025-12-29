plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
}

val jdkVersion = 21
val coreJar = project(":core").tasks.named("createPublicationJar")

dependencies {
    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    // add horizon api manually from the core project
    add("horizonHorizonApiConfig", project.files(coreJar.flatMap { (it as Jar).archiveFile }))
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