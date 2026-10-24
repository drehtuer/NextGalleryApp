plugins {
    alias(libs.plugins.nextgallery.android.library)
    alias(libs.plugins.nextgallery.android.hilt)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.media)
}
