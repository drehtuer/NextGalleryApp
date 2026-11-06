package net.drehtuer.nextgallery.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

/** Name of the task in every module that writes the JaCoCo XML report for SonarCloud. */
internal const val COVERAGE_REPORT_TASK = "coverageReport"

internal fun Project.configureJacoco() {
    pluginManager.apply("jacoco")
    extensions.getByType(JacocoPluginExtension::class.java).toolVersion = jacocoVersion()
    tasks.withType(Test::class.java).configureEach {
        extensions.configure(JacocoTaskExtension::class.java) {
            // Robolectric loads classes through its own class loader; without this they count as not covered.
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }
}

internal fun Project.jacocoVersion(): String = libs.findVersion("jacoco").get().requiredVersion
