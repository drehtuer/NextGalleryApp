import net.drehtuer.nextgallery.buildlogic.DependencyFloors
import org.gradle.api.Plugin
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.Settings

/**
 * Applies [DependencyFloors] to every configuration of the build: the build script classpaths, where the Gradle
 * plugins and their libraries live, and each project's configurations (lint, ktlint, detekt, annotation processors).
 */
class DependencyFloorsSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.gradle.beforeProject {
            buildscript.configurations.configureEach { raiseVulnerableVersions() }
            configurations.configureEach { raiseVulnerableVersions() }
        }
    }

    private fun Configuration.raiseVulnerableVersions() {
        resolutionStrategy.eachDependency {
            val version = DependencyFloors.raise(requested.group, requested.name, requested.version)
            if (version != null) {
                useVersion(version)
                because("Earlier versions have known vulnerabilities (D62).")
            }
        }
    }
}
