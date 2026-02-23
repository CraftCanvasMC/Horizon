package io.canvasmc.horizon.util

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.io.path.*

private val Iterable<File>.asPath
    get() = joinToString(File.pathSeparator) { it.absolutePath }

fun JavaLauncher.runJar(
    classpath: Iterable<File>,
    workingDir: Any,
    progress: ProgressLogger?,
    jvmArgs: List<String> = listOf(),
    vararg args: String
) {
    var mainClass: String? = null
    for (file in classpath) {
        mainClass = JarFile(file).use { jarFile ->
            jarFile.manifest.mainAttributes.getValue("Main-Class")
        } ?: continue
        break
    }
    if (mainClass == null) {
        throw RuntimeException("Could not determine main class name for ${classpath.asPath}")
    }

    val dir = workingDir.convertToPath()
    val output: OutputStream = progress?.let { ProgressLoggerOutputStream(it) } ?: UselessOutputStream

    val processBuilder = ProcessBuilder(
        this.executablePath.path.absolutePathString(),
        *jvmArgs.toTypedArray(),
        "-classpath",
        classpath.asPath,
        mainClass,
        *args
    ).directory(dir)

    val process = processBuilder.start()

    output.use {
        val outFuture = redirect(process.inputStream, it)
        val errFuture = redirect(process.errorStream, it)

        val exit = process.waitFor()
        outFuture.get(500L, TimeUnit.MILLISECONDS)
        errFuture.get(500L, TimeUnit.MILLISECONDS)
        if (exit != 0) {
            throw RuntimeException("Execution of '$mainClass' failed with exit code $exit. Classpath: ${classpath.asPath}")
        }
    }
}

class ProgressLoggerOutputStream(private val progress: ProgressLogger) : OutputStream() {
    private val buffer = kotlin.text.StringBuilder()

    override fun write(b: Int) {
        if (b.toChar() == '\n') {
            processLine(buffer.toString())
            buffer.setLength(0)
        } else {
            buffer.append(b.toChar())
        }
    }

    private fun processLine(line: String) {
        if (!line.startsWith("Applying AT")) return

        val regex = Regex("""Applying AT (\S+ \S+) .+:(\d+) to (\S+) of (\S+)$""")
        val match = regex.find(line) ?: return

        val type = match.groupValues[1]
        val member = match.groupValues[3]
        val clazz = match.groupValues[4]

        val fqMember = "$clazz.$member"
        progress.progress("Transforming $fqMember [$type]")
    }
}

fun Provider<JavaLauncher>.runJar(
    classpath: FileCollection,
    workingDir: Any,
    progress: ProgressLogger?,
    jvmArgs: List<String> = listOf(),
    vararg args: String
) {
    get().runJar(classpath, workingDir, progress, jvmArgs, *args)
}
