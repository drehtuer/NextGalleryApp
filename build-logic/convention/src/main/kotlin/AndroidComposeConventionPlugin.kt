import com.android.build.api.dsl.CommonExtension
import net.drehtuer.nextgallery.buildlogic.libs
import net.drehtuer.nextgallery.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        (extensions.getByName("android") as CommonExtension).buildFeatures.compose = true

        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            add("implementation", bom)
            add("implementation", libs.library("androidx-compose-ui"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("implementation", libs.library("androidx-compose-material3"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            add("testImplementation", bom)
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            // ui-test pulls in Espresso 3.5, which calls InputManager.getInstance() (removed in SDK 37).
            add("testImplementation", libs.library("androidx-test-espresso-core"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            add("androidTestImplementation", bom)
            add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("androidTestImplementation", libs.library("androidx-test-espresso-core"))
        }
    }
}
