import com.android.build.api.dsl.ApplicationExtension
import net.drehtuer.nextgallery.buildlogic.Namespaces
import net.drehtuer.nextgallery.buildlogic.ReleaseSigning
import net.drehtuer.nextgallery.buildlogic.appVersion
import net.drehtuer.nextgallery.buildlogic.configureAndroidCommon
import net.drehtuer.nextgallery.buildlogic.forbidProprietaryDependencies
import net.drehtuer.nextgallery.buildlogic.intVersion
import net.drehtuer.nextgallery.buildlogic.libs
import net.drehtuer.nextgallery.buildlogic.releaseSigning
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("nextgallery.android.test")
        pluginManager.apply("nextgallery.detekt")
        pluginManager.apply("nextgallery.ktlint")

        val version = appVersion()
        val signing = releaseSigning()
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                applicationId = Namespaces.ROOT
                targetSdk = libs.intVersion("androidTargetSdk")
                versionCode = version.code
                versionName = version.name
            }
            signing?.let { configureReleaseSigning(this@with, it) }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }
            }
        }
        forbidProprietaryDependencies()
    }

    /** Without `keystore.properties` the release build stays unsigned. */
    private fun ApplicationExtension.configureReleaseSigning(project: Project, values: ReleaseSigning) {
        val config = signingConfigs.create("release") {
            storeFile = project.rootProject.file(values.storeFile)
            storePassword = values.storePassword
            keyAlias = values.keyAlias
            keyPassword = values.keyPassword
        }
        buildTypes.getByName("release").signingConfig = config
    }
}
