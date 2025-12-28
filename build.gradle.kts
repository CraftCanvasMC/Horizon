plugins {
    java
    idea
    alias(libs.plugins.userdev) apply false
    alias(libs.plugins.shadow) apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
}
