version = project.version.toString() + "." + fetchBuild().get()

fun fetchBuild(): Provider<String> {
    return providers.gradleProperty("buildNumber")
        .orElse(providers.environmentVariable("BUILD_NUMBER"))
        .orElse("local")
}
