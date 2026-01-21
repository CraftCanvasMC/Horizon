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
