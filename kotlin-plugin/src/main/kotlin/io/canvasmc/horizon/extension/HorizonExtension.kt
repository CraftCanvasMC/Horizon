package io.canvasmc.horizon.extension

import io.canvasmc.horizon.util.constants.CANVAS_MAVEN_REPO_URL
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class HorizonExtension @Inject constructor(objects: ObjectFactory, project: Project) {
    /**
     * Access transformer files to apply to the server jar.
     */
    abstract val accessTransformerFiles: ConfigurableFileCollection

    /**
     * The repository from which JST should be resolved.
     */
    val jstRepo: Property<String> = objects.property<String>().convention(CANVAS_MAVEN_REPO_URL)

    /**
     * Whether to automatically inject Canvas's maven repository for easier Horizon API resolution.
     */
    val injectCanvasRepository: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Whether to fail the build if an AT fails to be applied to the Minecraft source.
     */
    val failFastOnUnapplicableAT: Property<Boolean> = objects.property<Boolean>().convention(true)
}
