package net.drehtuer.nextgallery.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal fun Project.configureAndroidCommon(android: CommonExtension) {
    android.namespace = Namespaces.forProject(path)
    android.compileSdk = libs.intVersion("androidCompileSdk")
    android.compileSdkMinor = 0
    android.defaultConfig.minSdk = libs.intVersion("androidMinSdk")
    android.lint.apply {
        abortOnError = true
        warningsAsErrors = true
        lintConfig = rootProject.file("config/lint/lint.xml")
    }
    configureKotlinToolchain()
}

internal fun Project.configureKotlinToolchain() {
    (extensions.getByName("kotlin") as KotlinBaseExtension).jvmToolchain(libs.intVersion("jvmToolchain"))
}
