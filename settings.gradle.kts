pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Canvas"
            url = uri("https://maven.canvasmc.io/snapshots")
        }
    }
    includeBuild("kotlin-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "horizon"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("core")
include("test-plugin")