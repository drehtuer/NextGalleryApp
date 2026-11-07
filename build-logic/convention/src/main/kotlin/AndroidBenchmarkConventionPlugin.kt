import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import net.drehtuer.nextgallery.buildlogic.configureAndroidCommon
import net.drehtuer.nextgallery.buildlogic.intVersion
import net.drehtuer.nextgallery.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Macrobenchmarks and the Baseline Profile generator, which need a real device. The debug variant is switched off: it
 * can't measure anything, and without it `assembleDebug` and `connectedDebugAndroidTest` leave this module alone.
 */
class AndroidBenchmarkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")
        pluginManager.apply("nextgallery.detekt")
        pluginManager.apply("nextgallery.ktlint")

        extensions.configure<TestExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                targetSdk = libs.intVersion("androidTargetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            targetProjectPath = ":app"
            @Suppress("UnstableApiUsage")
            experimentalProperties["android.experimental.self-instrumenting"] = true
        }
        extensions.configure<TestAndroidComponentsExtension> {
            beforeVariants(selector().withBuildType("debug")) { it.enable = false }
        }
    }
}
