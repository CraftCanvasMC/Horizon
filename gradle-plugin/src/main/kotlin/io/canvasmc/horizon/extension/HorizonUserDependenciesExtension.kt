package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.constants.HORIZON_API_ARTIFACT_ID
import io.canvasmc.horizon.util.constants.HORIZON_API_CONFIG
import io.canvasmc.horizon.util.constants.HORIZON_API_GROUP
import io.canvasmc.horizon.util.constants.HORIZON_API_SINGLE_CONFIG
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
     * Adds a dependency on Horizon's API to the project [org.gradle.api.artifacts.Configuration].
     *
     * @param version dependency version
     * @param group dependency group
     * @param artifactId dependency artifactId
     * @param configurationsNames names of the Horizon API [org.gradle.api.artifacts.Configuration]
     * @param configurationAction action configuring the dependency
     * @return dependency
     */
    @JvmOverloads
    fun horizonApi(
        version: String? = null,
        group: String = HORIZON_API_GROUP,
        artifactId: String = HORIZON_API_ARTIFACT_ID,
        configurationsNames: List<String> = listOf(HORIZON_API_CONFIG, HORIZON_API_SINGLE_CONFIG),
        configurationAction: Action<ExternalModuleDependency> = nullAction()
    ): ExternalModuleDependency {
        val dep = dependencyFactory.create(buildDependencyString(group, artifactId, version))
        configurationAction(dep)
        configurationsNames.forEach {
            dependencies.add(it, dep)
        }
        return dep
    }

    /**
     * Adds a dependency on Horizon API to the [HORIZON_API_CONFIG] and [HORIZON_API_SINGLE_CONFIG] configurations.
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
        dependencies.addProvider(
            HORIZON_API_SINGLE_CONFIG,
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
     * @param configurationsNames names of the Horizon API [org.gradle.api.artifacts.Configuration]
     * @param configurationAction action configuring the dependency
     */
    @JvmOverloads
    fun horizonApi(
        project: ProjectDependency,
        configurationsNames: List<String> = listOf(HORIZON_API_CONFIG, HORIZON_API_SINGLE_CONFIG),
        configurationAction: Action<ProjectDependency> = nullAction()
    ): ProjectDependency {
        configurationAction(project)
        configurationsNames.forEach {
            dependencies.add(it, project)
        }
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
