import net.drehtuer.nextgallery.buildlogic.FeatureDependencyRule
import net.drehtuer.nextgallery.buildlogic.libs
import net.drehtuer.nextgallery.buildlogic.library
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("nextgallery.android.library")
        pluginManager.apply("nextgallery.android.compose")
        pluginManager.apply("nextgallery.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("androidx-hilt-navigation-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("kotlinx-serialization-json"))
        }

        forbidFeatureToFeatureDependencies()
    }

    private fun Project.forbidFeatureToFeatureDependencies() {
        configurations.configureEach {
            dependencies.withType(ProjectDependency::class.java).configureEach {
                if (FeatureDependencyRule.isForbidden(this@forbidFeatureToFeatureDependencies.path, path)) {
                    throw GradleException(
                        "${this@forbidFeatureToFeatureDependencies.path} must not depend on $path: " +
                            "feature modules may only depend on core modules.",
                    )
                }
            }
        }
    }
}
