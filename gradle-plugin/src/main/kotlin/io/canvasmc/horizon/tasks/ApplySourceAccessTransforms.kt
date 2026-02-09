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

    @get:Classpath
    abstract val mappedServerJar: RegularFileProperty

    @get:OutputFile
    abstract val sourceTransformedMappedServerJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val atFile: RegularFileProperty

    @get:Input
    abstract val validateATs: Property<Boolean>

    @get:Internal
    abstract val timeSpent: Property<Long>

    @TaskAction
    fun run() {
        val inputJar = mappedServerJar.path
        val outputJar = sourceTransformedMappedServerJar.path.cleanFile()
        if (atFile.path.readText().isNotBlank()) {
            val generatedIn = measureNanoTime {
                println("Applying access transformers 1/2...")
                ats.run(
                    launcher.get(),
                    inputJar,
                    outputJar,
                    atFile.path,
                    temporaryDir.toPath(),
                    validateATs.get(),
                )
            }
            timeSpent.set(generatedIn)
            println("Done in ${formatNs(timeSpent.get())}!")
        } else {
            inputJar.copyTo(outputJar)
        }
    }
}
