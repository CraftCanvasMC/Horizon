package io.canvasmc.horizon.functional

import io.papermc.paperweight.util.*
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FunctionalityTest {

    val debug = true

    @Test
    fun `test simple functionality`(@TempDir(cleanup = CleanupMode.ON_SUCCESS) tempDir: Path) {
        println("running tests in $tempDir")
        val testDir = Paths.get("src/test/resources/functionality")

        val runner = tempDir.copyProject("functionality").gradleRunner()

        println("\nrunning build dependencies\n")
        val build = runner
            .withArguments("build", "dependencies", "--stacktrace")
            .withDebug(debug)
            .build()

        assertEquals(build.task(":build")?.outcome, TaskOutcome.SUCCESS)

        println("\nchecking if the jar file is correctly transformed")
        val atLog = tempDir.resolve("build/tmp/applySourceAccessTransforms/log.txt").readText()

        assertFalse(atLog.contains("did not apply as its target doesn't exist"))

        println("\nverifying if the merged at file matches expected")
        assertEquals(tempDir.resolve(".gradle/caches/horizon/taskCache/merged.at").readText(), tempDir.resolve("build-data/expected-merged.at").readText())

        println("\nadding broken ATs\n")
        modifyFile(tempDir.resolve("src/main/resources/wideners.at")) {
            it.replace(
                "# broken ats go here",
                "public-f com.testingstuff.abcdefgh randomMethod"
            )
        }

        println("\nrunning build dependencies again with broken ATs\n")
        val build1 = runner
            .withArguments("build", "dependencies", "--stacktrace")
            .withDebug(debug)
            .buildAndFail()

        assertEquals(build1.task(":applySourceAccessTransforms")?.outcome, TaskOutcome.FAILED)

        println("\nverifying again if the merged at file matches expected")
        assertEquals(tempDir.resolve(".gradle/caches/horizon/taskCache/merged.at").readText(), tempDir.resolve("build-data/expected-merged2.at").readText())

        println("\nrunning build dependencies with ignore broken ATs\n")
        modifyFile(tempDir.resolve("build.gradle.kts")) {
            it.replace("// failFastOnUnapplicableAT = false", "failFastOnUnapplicableAT = false")
        }
        val build2 = runner
            .withArguments("build", "dependencies", "--stacktrace")
            .withDebug(debug)
            .build()

        assertEquals(build2.task(":build")?.outcome, TaskOutcome.SUCCESS)

        println("\nchecking if the jar file has errors while transforming")
        val atLog1 = tempDir.resolve("build/tmp/applySourceAccessTransforms/log.txt").readText()

        assertContains(atLog1, "did not apply as its target doesn't exist")
    }

    fun modifyFile(path: Path, action: (content: String) -> String) {
        path.writeText(action.invoke(path.readText()))
    }

    fun Path.copyProject(resourcesProjectName: String): ProjectFiles {
        Paths.get("src/test/resources/$resourcesProjectName")
            .copyRecursivelyTo(this)
        Git(this)("init").executeSilently()
        return ProjectFiles(this)
    }

    class ProjectFiles(val projectDir: Path) {
        val zipFile = projectDir.resolve("build-data/userdev.zip").toFile()
        val unzipDir = projectDir.resolve("build-data/userdev").toFile()

        val jarFile: File by lazy {
            unzip(zipFile, unzipDir)
            unzipDir.walk().first { it.extension == "jar" }
        }

        val baseClasspath = GradleRunner.create().withPluginClasspath().pluginClasspath
        val weaverClasspath = listOf(jarFile)

        fun gradleRunner(): GradleRunner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withPluginClasspath(baseClasspath + weaverClasspath)
            .withProjectDir(projectDir.toFile())
    }
}
