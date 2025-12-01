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

package io.canvasmc.horizon.tasks

import io.canvasmc.horizon.util.defaultOutput
import io.papermc.paperweight.util.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import paper.libs.org.cadixdev.at.AccessTransformSet
import paper.libs.org.cadixdev.at.io.AccessTransformFormats

abstract class MergeAccessTransformers : BaseTask() {

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    override fun init() {
        outputFile.convention(defaultOutput("at"))
    }

    @TaskAction
    fun run() {
        val ats = files.asFileTree
            .files
            .asSequence()
            .filter { it.exists() }
            .map { AccessTransformFormats.FML.read(it.toPath()) }

        val outputAt = AccessTransformSet.create()
        for (at in ats) {
            outputAt.merge(at)
        }

        AccessTransformFormats.FML.writeLF(outputFile.path, outputAt)
    }
}
