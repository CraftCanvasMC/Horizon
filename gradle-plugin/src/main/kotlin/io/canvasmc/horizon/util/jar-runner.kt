/*
 * This file is part of the paperweight gradle plugin.
 *
 * paperweight is a Gradle plugin for the PaperMC project.
 *
 * Copyright (c) 2023 Kyle Wood (DenWav)
 *                    Contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 only, no later versions.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

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

        val memberRegex =
            Regex("""Applying AT (\S+ \S+) .+ to (\S+) of (\S+)$""")

        val classRegex =
            Regex("""Applying AT (\S+ \S+) .+ to (\S+)$""")

        val memberMatch = memberRegex.find(line)
        if (memberMatch != null) {
            val type = memberMatch.groupValues[1]
            val member = memberMatch.groupValues[2]
            val clazz = memberMatch.groupValues[3]
            progress.progress("Transforming $clazz.$member [$type]")
            return
        }

        val classMatch = classRegex.find(line)
        if (classMatch != null) {
            val type = classMatch.groupValues[1]
            val target = classMatch.groupValues[2]
            progress.progress("Transforming $target [$type]")
        }
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
