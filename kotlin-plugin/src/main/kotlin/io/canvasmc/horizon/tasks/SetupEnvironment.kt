package io.canvasmc.horizon.tasks

import io.canvasmc.horizon.util.*
import kotlin.io.path.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import kotlin.system.measureNanoTime

@CacheableTask
abstract class SetupEnvironment : JavaLauncherTask() {

    @get:Nested
    val ats: ApplySourceATs = objects.newInstance()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val processedServerJar: RegularFileProperty

    @get:OutputFile
    abstract val transformedServerJar: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val atFile: RegularFileProperty

    @get:Internal
    abstract val timeSpent: Property<Long>

    @TaskAction
    fun run() {
        println("Preparing the dev environment...")

        val generatedIn = measureNanoTime {
            val inputJar = processedServerJar.get().path
            val outputJar = transformedServerJar.get().path.cleanFile()

            if (atFile.isPresent && atFile.path.readText().isNotBlank()) {
                println("Applying access transformers...")
                ats.run(
                    launcher.get(),
                    inputJar,
                    outputJar,
                    atFile.path,
                    temporaryDir.toPath(),
                    singleFile = true,
                )
            } else {
                inputJar.copyTo(outputJar)
            }
        }
        timeSpent.set(generatedIn)
        println("Done preparing in ${formatNs(timeSpent.get())}!")
    }
}