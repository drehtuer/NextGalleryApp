package net.drehtuer.nextgallery.buildlogic

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.Instant

/** Copies the APK AGP built, named by [ApkNames], to its own directory `build/outputs/nextgallery/<variant>`. */
@DisableCachingByDefault(because = "Copying a file is faster than caching it.")
abstract class NamedApkTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val debug: Property<Boolean>

    @get:Input
    abstract val signed: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        val apk = builtArtifactsLoader.get().load(apkDirectory.get())?.elements?.singleOrNull()?.outputFile
            ?: throw GradleException("Expected exactly one APK in ${apkDirectory.get().asFile}.")
        val name = if (debug.get()) {
            ApkNames.debug(versionName.get(), Instant.now())
        } else {
            ApkNames.release(versionName.get(), signed.get())
        }
        val copy = ApkNames.copy(File(apk), outputDirectory.get().asFile, name)
        logger.lifecycle("APK: ${copy.absolutePath}")
    }
}
