plugins {
    alias(libs.plugins.nextgallery.android.application)
    alias(libs.plugins.nextgallery.android.compose)
    alias(libs.plugins.nextgallery.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.baselineprofile)
}

// The profile is generated on a device and committed; a normal build only reads it, if there is one.
baselineProfile {
    automaticGenerationDuringBuild = false
}

// The plugin's extra build types are only for running on a device; their unit tests would just repeat release.
androidComponents {
    beforeVariants(selector().withBuildType("nonMinifiedRelease")) {
        it.hostTests.values.forEach { t -> t.enable = false }
    }
    beforeVariants(selector().withBuildType("benchmarkRelease")) {
        it.hostTests.values.forEach { t -> t.enable = false }
    }
}

// The licence list of About & licences comes from the POMs of what really ships, as a raw resource.
aboutLibraries {
    collect {
        fetchRemoteLicense = false
        fetchRemoteFunding = false
    }
    export {
        prettyPrint = false
        excludeFields.addAll("developers", "funding", "description", "organization", "scm", "website")
    }
    license {
        // Anything else fails the build: the app is GPL-3.0 and every shipped library must be compatible.
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause")
    }
}

android {
    buildFeatures {
        // The version shown in About & licences.
        buildConfig = true
    }
}

dependencies {
    baselineProfile(projects.benchmark)

    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.platform)
    implementation(projects.core.nextcloud)
    implementation(projects.feature.folders)
    implementation(projects.feature.timeline)
    implementation(projects.feature.map)
    implementation(projects.feature.viewer)
    implementation(projects.feature.editor)
    implementation(projects.feature.remote)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.serialization.json)
}
