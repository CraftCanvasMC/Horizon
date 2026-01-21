/*
 * Based off of the gremlin plugin by jpenilla.
 *
 * gremlin
 *
 * Copyright (c) 2025 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

abstract class CollectDependenciesTask : DefaultTask() {

    @get:Inject
    abstract val objects: ObjectFactory

    @get:Nested
    val artifacts: Artifacts = objects.newInstance(Artifacts::class.java)

    @get:Input
    abstract val repositoryData: ListProperty<RepositoryData>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    data class RepositoryData(
        val name: String,
        val url: String
    ) : java.io.Serializable

    @Suppress("unused")
    fun setFrom(configuration: Provider<Configuration>) {
        artifacts.setFrom(configuration.map { it.incoming.artifacts })
    }

    @TaskAction
    fun collect() {
        val dest = outputDir.get().asFile
        dest.mkdirs()

        val lines = artifacts.artifacts().mapNotNull { artifact ->
            val component = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
            val groupPath = component.group.replace('.', '/')
            val mavenPath = "$groupPath/${component.module}/${component.version}/${artifact.file.name}"
            val sha256 = artifact.file.sha256()
            "${component.group}:${component.module}:${component.version}\t$mavenPath\t$sha256"
        }

        dest.resolve("artifacts.context").writeText(lines.joinToString("\n"))

        dest.resolve("repositories.context").writeText(
            repositoryData.get().joinToString("\n") { "${it.name}\t${it.url}" }
        )
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    abstract class Artifacts {

        data class Artifact(
            val id: ComponentArtifactIdentifier,
            val variant: ResolvedVariantResult,
            val file: File
        )

        @get:Internal
        abstract val componentArtifactIdentifiers: ListProperty<ComponentArtifactIdentifier>

        @get:Internal
        abstract val resolvedVariantResults: ListProperty<ResolvedVariantResult>

        @get:Internal
        abstract val filesInternal: ListProperty<File>

        @get:InputFiles
        @get:Optional
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val files: ConfigurableFileCollection

        fun artifacts(): List<Artifact> {
            val ids = componentArtifactIdentifiers.get()
            val variants = resolvedVariantResults.get()
            val files = filesInternal.get()
            if (setOf(ids.size, variants.size, files.size).size != 1) {
                throw IllegalStateException("Mismatch between artifact input list lengths (ids: ${ids.size}, variants: ${variants.size}, files: ${files.size})")
            }
            return ids.mapIndexed { idx, id -> Artifact(id, variants[idx], files[idx]) }
        }

        fun setFrom(artifactCollection: Provider<ArtifactCollection>) {
            files.setFrom(artifactCollection.map { it.artifactFiles })
            val artifactsSorted = artifactCollection.flatMap {
                it.resolvedArtifacts.map { resolvedArtifacts ->
                    resolvedArtifacts.sortedWith(
                        Comparator.comparing<ResolvedArtifactResult, String> { artifact -> artifact.id.componentIdentifier.displayName }
                            .thenComparing { artifact -> artifact.file.name }
                    )
                }
            }
            componentArtifactIdentifiers.set(artifactsSorted.map { list -> list.map { it.id } })
            resolvedVariantResults.set(artifactsSorted.map { list -> list.map { it.variant } })
            filesInternal.set(artifactsSorted.map { list -> list.map { it.file } })
        }
    }
}
