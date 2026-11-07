import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import net.drehtuer.nextgallery.buildlogic.AppVersion
import net.drehtuer.nextgallery.buildlogic.NamedApkTask
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
import org.gradle.kotlin.dsl.register

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
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants(selector().withBuildType("debug")) { registerNamedApk(it, version, debug = true, signed = true) }
            onVariants(selector().withBuildType("release")) {
                registerNamedApk(it, version, debug = false, signed = signing != null)
            }
        }
    }

    /** `assembleDebug` and `assembleRelease` also write the APK as `NextGallery-...apk` (see `ApkNames`). */
    private fun Project.registerNamedApk(
        variant: ApplicationVariant,
        version: AppVersion,
        debug: Boolean,
        signed: Boolean,
    ) {
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val namedApk = tasks.register<NamedApkTask>("namedApk$variantName") {
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            versionName.set(version.name)
            this.debug.set(debug)
            this.signed.set(signed)
            outputDirectory.set(layout.buildDirectory.dir("outputs/nextgallery/${variant.name}"))
        }
        tasks.matching { it.name == "assemble$variantName" }.configureEach { finalizedBy(namedApk) }
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
