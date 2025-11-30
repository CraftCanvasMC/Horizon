plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "horizon"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("core")
include("api")
//include("kotlin-plugin")