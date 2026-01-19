plugins {
    java
    idea
    alias(libs.plugins.userdev) apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.isFork = true
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).tags("apiNote:a:API Note:")
    }

    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
}

// utilities for accessing the plugin project
tasks.register("buildPlugin") {
    dependsOn(gradle.includedBuild("kotlin-plugin").task(":build"))
}

tasks.register("publishPlugin") {
    dependsOn(gradle.includedBuild("kotlin-plugin").task(":publishAllPublicationsToCanvasmcRepository"))
}

tasks.register("publishSnapshotPlugin") {
    dependsOn(gradle.includedBuild("kotlin-plugin").task(":publishAllPublicationsToSnapshotsRepository"))
}

tasks.register("publishPluginLocally") {
    dependsOn(gradle.includedBuild("kotlin-plugin").task(":publishToMavenLocal"))
}

tasks.register("formatPlugin") {
    dependsOn(gradle.includedBuild("kotlin-plugin").task(":format"))
}
