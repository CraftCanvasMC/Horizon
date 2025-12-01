package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.providerSet
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.*

abstract class HorizonExtension @Inject constructor(objects: ObjectFactory, project: Project) {
    /**
     * Access transformer files to apply to the server jar.
     */
    abstract val accessTransformerFiles: ConfigurableFileCollection
    /**
     * Configurations to add the Minecraft server dependency to.
     */
    val addServerDependencyTo: SetProperty<Configuration> = objects.setProperty<Configuration>().convention(
        objects.providerSet(
            project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
            project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
        )
    )
}
