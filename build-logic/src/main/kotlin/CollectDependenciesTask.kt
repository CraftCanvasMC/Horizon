import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

abstract class CollectDependenciesTask : DefaultTask() {

    @get:Internal
    abstract val artifactIds: ListProperty<ComponentArtifactIdentifier>

    @get:Internal
    abstract val artifactFilesInternal: ListProperty<File>

    @get:InputFiles
    @get:Classpath
    abstract val artifactFiles: ConfigurableFileCollection

    @get:Input
    abstract val repositoryData: ListProperty<RepositoryData>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    data class RepositoryData(
        val name: String,
        val url: String
    ) : java.io.Serializable

    fun setFrom(configuration: Provider<Configuration>) {
        val artifacts = configuration.map { it.incoming.artifacts }

        artifactFiles.setFrom(artifacts.map { it.artifactFiles })

        val sorted = artifacts.flatMap {
            it.resolvedArtifacts.map { resolved ->
                resolved.sortedWith(
                    compareBy(
                        { it.id.componentIdentifier.displayName },
                        { it.file.name }
                    )
                )
            }
        }

        artifactIds.set(sorted.map { it.map { it.id } })
        artifactFilesInternal.set(sorted.map { it.map { it.file } })
    }

    @TaskAction
    fun collect() {
        val dest = outputDir.get().asFile
        dest.mkdirs()

        val ids = artifactIds.get()
        val files = artifactFilesInternal.get()

        val lines = mutableListOf<String>()

        ids.zip(files).forEach { (id, file) ->
            val component = id.componentIdentifier as? ModuleComponentIdentifier ?: return@forEach

            val groupPath = component.group.replace('.', '/')
            val mavenPath = "$groupPath/${component.module}/${component.version}/${file.name}"

            val sha256 = file.sha256()

            lines += "${component.group}:${component.module}:${component.version}\t$mavenPath\t$sha256"
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
}
