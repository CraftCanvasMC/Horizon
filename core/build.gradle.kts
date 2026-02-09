plugins {
    id("io.canvasmc.weaver.userdev")
    id("publishing-conventions")
    id("versioning-conventions")
    id("io.canvasmc.horizon")
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

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

horizon {
    // needed for CI
    validateATs = false
    accessTransformerFiles = files("src/main/resources/internal.at")
}

tasks.withType<Javadoc> {
    exclude("io/canvasmc/horizon/inject/mixin/**")
    exclude("**/taskCache/**")
}
