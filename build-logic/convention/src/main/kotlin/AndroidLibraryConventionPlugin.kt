import com.android.build.api.dsl.LibraryExtension
import net.drehtuer.nextgallery.buildlogic.configureAndroidCommon
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("nextgallery.android.test")
        pluginManager.apply("nextgallery.detekt")
        pluginManager.apply("nextgallery.ktlint")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
        }
    }
}
