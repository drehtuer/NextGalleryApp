import com.android.build.api.dsl.ApplicationExtension
import net.drehtuer.nextgallery.buildlogic.Namespaces
import net.drehtuer.nextgallery.buildlogic.configureAndroidCommon
import net.drehtuer.nextgallery.buildlogic.intVersion
import net.drehtuer.nextgallery.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("nextgallery.android.test")
        pluginManager.apply("nextgallery.detekt")
        pluginManager.apply("nextgallery.ktlint")

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                applicationId = Namespaces.ROOT
                targetSdk = libs.intVersion("androidTargetSdk")
                versionCode = 1
                versionName = "0.1.0"
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }
            }
        }
    }
}
