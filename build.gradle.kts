plugins {
    java
    idea
    alias(libs.plugins.userdev) apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

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


tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// utilities for accessing the plugin project
tasks.register("buildPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":build"))
}

tasks.register("publishPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":publishAllPublicationsToCanvasmcRepository"))
}

tasks.register("publishSnapshotPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":publishAllPublicationsToSnapshotsRepository"))
}

tasks.register("publishPluginLocally") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":publishToMavenLocal"))
}

tasks.register("formatPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":format"))
}
