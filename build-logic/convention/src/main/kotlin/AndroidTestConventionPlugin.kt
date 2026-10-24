import com.android.build.api.dsl.CommonExtension
import net.drehtuer.nextgallery.buildlogic.libs
import net.drehtuer.nextgallery.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies

/** Shared test setup for Android modules: JVM tests (incl. Robolectric) and instrumented tests. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val android = extensions.getByName("android") as CommonExtension
        android.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        android.testOptions.unitTests.apply {
            isIncludeAndroidResources = true
            all { test -> test.jvmArgs(ROBOLECTRIC_JVM_ARGS) }
        }

        // Modules without tests yet still get generated test sources (e.g. from Hilt).
        tasks.withType(Test::class.java).configureEach { failOnNoDiscoveredTests.set(false) }

        dependencies {
            add("testImplementation", libs.library("junit4"))
            add("testImplementation", libs.library("mockk"))
            add("testImplementation", libs.library("turbine"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("androidx-test-core"))
            add("testImplementation", libs.library("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.library("androidx-test-runner"))
            add("androidTestImplementation", libs.library("androidx-test-core"))
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
        }
    }

    private companion object {
        // Required by Robolectric on JDK 17+ (see https://robolectric.org/getting-started/).
        val ROBOLECTRIC_JVM_ARGS = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.security=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        )
    }
}
