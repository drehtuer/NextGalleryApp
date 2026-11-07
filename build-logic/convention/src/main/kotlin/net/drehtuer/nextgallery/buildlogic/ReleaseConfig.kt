package net.drehtuer.nextgallery.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * `-PversionTag=v1.2.3` wins, then the nearest tag in the Git history. Git failing (not installed, no tags, a worktree
 * whose `.git` points outside a container) is not an error: such a build gets [AppVersion.FALLBACK_TAG].
 */
internal fun Project.appVersion(): AppVersion {
    val tag = providers.gradleProperty("versionTag").orNull ?: nearestGitTag() ?: AppVersion.FALLBACK_TAG
    return AppVersion.fromTag(tag)
}

private fun Project.nearestGitTag(): String? = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().takeIf { it.isNotEmpty() }
}.getOrNull()

internal fun Project.releaseSigning(): ReleaseSigning? = ReleaseSigning.parse(
    providers.fileContents(rootProject.layout.projectDirectory.file(ReleaseSigning.FILE_NAME)).asText.orNull,
)

/** Fails the build as soon as a proprietary library gets into the app, directly or through another library. */
internal fun Project.forbidProprietaryDependencies() {
    configurations.matching { ForbiddenDependencies.isShipped(it.name) }.configureEach {
        val configuration = name
        resolutionStrategy.eachDependency {
            if (ForbiddenDependencies.isForbidden(requested.group)) {
                throw GradleException("$requested must not ship in the app (found in $configuration).")
            }
        }
    }
}
