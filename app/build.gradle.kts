plugins {
    alias(libs.plugins.nextgallery.android.application)
    alias(libs.plugins.nextgallery.android.compose)
    alias(libs.plugins.nextgallery.android.hilt)
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.core.platform)
    implementation(projects.feature.folders)
    implementation(projects.feature.timeline)
    implementation(projects.feature.map)
    implementation(projects.feature.viewer)
    implementation(projects.feature.editor)
    implementation(projects.feature.remote)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
