package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.constants.HORIZON_API_ARTIFACT_ID
import io.canvasmc.horizon.util.constants.HORIZON_API_CONFIG
import io.canvasmc.horizon.util.constants.HORIZON_API_GROUP
import io.canvasmc.horizon.util.constants.MIXIN_PLUGIN_IMPLEMENTATION_CONFIG
import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.invoke
import javax.inject.Inject

@Suppress("unused")
abstract class HorizonUserDependenciesExtension @Inject constructor(
    private val dependencies: DependencyHandler,
    private val dependencyFactory: DependencyFactory,
) {
    /**
     * Adds a dependency on Horizon's API to the project Horizon API [org.gradle.api.artifacts.Configuration].
     *
     * @param version dependency version
     * @param group dependency group
     * @param artifactId dependency artifactId
     * @param configurationsName name of the Horizon API [org.gradle.api.artifacts.Configuration]
     * @param configurationAction action configuring the dependency
     * @return dependency
     */
    @JvmOverloads
    fun horizonApi(
        version: String? = null,
        group: String = HORIZON_API_GROUP,
        artifactId: String = HORIZON_API_ARTIFACT_ID,
        configurationName: String = HORIZON_API_CONFIG,
        configurationAction: Action<ExternalModuleDependency> = nullAction()
    ): ExternalModuleDependency {
        val dep = dependencyFactory.create(buildDependencyString(group, artifactId, version))
        configurationAction(dep)
        dependencies.add(configurationName, dep)
        return dep
    }

    /**
     * Adds a dependency on Horizon API to the [HORIZON_API_CONFIG] configuration.
     *
     * Intended for use with Gradle version catalogs.
     *
     * @param version version provider
     * @param configurationAction action configuring the dependency
     */
    @JvmOverloads
    fun horizonApi(
        version: Provider<String>,
        configurationAction: Action<ExternalModuleDependency> = nullAction()
    ) {
        dependencies.addProvider(
            HORIZON_API_CONFIG,
            version.map { "$HORIZON_API_GROUP:$HORIZON_API_ARTIFACT_ID:$it" },
            configurationAction
        )
    }

    /**
     * Adds a dependency on Horizon's API to the project [org.gradle.api.artifacts.Configuration].
     *
     * Intended for use in builds containing Horizon API as a subproject.
     *
     * @param project project dependency [org.gradle.api.artifacts.ProjectDependency]
     * @param configurationsName name of the Horizon API [org.gradle.api.artifacts.Configuration]
     * @param configurationAction action configuring the dependency
     */
    @JvmOverloads
    fun horizonApi(
        project: ProjectDependency,
        configurationName: String = HORIZON_API_CONFIG,
        configurationAction: Action<ProjectDependency> = nullAction()
    ): ProjectDependency {
        configurationAction(project)
        dependencies.add(configurationName, project)
        return project
    }

    /**
     * Creates a dependency on Horizon's API without adding it to any configurations.
     *
     * @param version dependency version
     * @param group dependency group
     * @param artifactId dependency artifactId
     * @param configurationAction action configuring the dependency
     * @return dependency
     */
    @JvmOverloads
    fun horizonApiDependency(
        version: String? = null,
        group: String = HORIZON_API_GROUP,
        artifactId: String = HORIZON_API_ARTIFACT_ID,
        configurationAction: Action<ExternalModuleDependency> = nullAction()
    ): ExternalModuleDependency {
        val dep = dependencyFactory.create(buildDependencyString(group, artifactId, version))
        configurationAction(dep)
        return dep
    }

    /**
     * Adds a provided mixin plugin dependency to the project.
     *
     * The dependency is exposed on the compile classpath and added to run-paper plugin loading,
     * but is not embedded into the produced plugin jar.
     */
    @JvmOverloads
    fun mixinPluginImplementation(
        version: String? = null,
        group: String,
        artifactId: String,
        configurationName: String = MIXIN_PLUGIN_IMPLEMENTATION_CONFIG,
        configurationAction: Action<ExternalModuleDependency> = nullAction()
    ): ExternalModuleDependency {
        val dep = dependencyFactory.create(buildDependencyString(group, artifactId, version))
        configurationAction(dep)
        dependencies.add(configurationName, dep)
        return dep
    }

    /**
     * Adds a provided mixin plugin dependency to the default configuration.
     *
     * Intended for use with Gradle version catalogs.
     */
    @JvmOverloads
    fun mixinPluginImplementation(
        dependencyNotation: Provider<String>,
        configurationAction: Action<ExternalModuleDependency> = nullAction()
    ) {
        dependencies.addProvider(
            MIXIN_PLUGIN_IMPLEMENTATION_CONFIG,
            dependencyNotation,
            configurationAction
        )
    }

    /**
     * Adds a provided mixin plugin dependency from a local project.
     */
    @JvmOverloads
    fun mixinPluginImplementation(
        project: ProjectDependency,
        configurationName: String = MIXIN_PLUGIN_IMPLEMENTATION_CONFIG,
        configurationAction: Action<ProjectDependency> = nullAction()
    ): ProjectDependency {
        configurationAction(project)
        dependencies.add(configurationName, project)
        return project
    }

    // taken from paperweight-userdev
    @Suppress("unchecked_cast")
    private fun <T : Any> nullAction(): Action<T> = NullAction as Action<T>

    private object NullAction : Action<Any> {
        override fun execute(t: Any) {}
    }

    private fun buildDependencyString(
        group: String,
        artifactId: String,
        version: String?
    ): String {
        val s = StringBuilder(group)
            .append(':')
            .append(artifactId)
        if (version != null) {
            s.append(':').append(version)
        }
        return s.toString()
    }
}
