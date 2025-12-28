package io.canvasmc.horizon.tasks

import io.canvasmc.horizon.util.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import kotlin.io.path.*
import kotlin.system.measureNanoTime

@CacheableTask
abstract class ApplySourceAccessTransforms : JavaLauncherTask() {

    @get:Nested
    val ats: ApplySourceATs = objects.newInstance()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mappedServerJar: RegularFileProperty

    @get:OutputFile
    abstract val sourceTransformedMappedServerJar: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val atFile: RegularFileProperty

    @get:Input
    abstract val failFast: Property<Boolean>

    @get:Internal
    abstract val timeSpent: Property<Long>

    @TaskAction
    fun run() {
        val generatedIn = measureNanoTime {
            val inputJar = mappedServerJar.get().path
            val outputJar = sourceTransformedMappedServerJar.get().path.cleanFile()

            if (atFile.isPresent && atFile.path.readText().isNotBlank()) {
                println("Applying access transformers 1/2...")
                ats.run(
                    launcher.get(),
                    inputJar,
                    outputJar,
                    atFile.path,
                    temporaryDir.toPath(),
                    archive = true,
                    validation = failFast.get(),
                )
            } else {
                inputJar.copyTo(outputJar)
            }
        }
        timeSpent.set(generatedIn)
        println("Done in ${formatNs(timeSpent.get())}!")
    }
}
