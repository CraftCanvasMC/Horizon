pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "horizon"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("core")
include("kotlin-plugin")
include("test-plugin")