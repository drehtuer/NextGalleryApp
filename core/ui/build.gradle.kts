plugins {
    alias(libs.plugins.nextgallery.android.library)
    alias(libs.plugins.nextgallery.android.compose)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
}
