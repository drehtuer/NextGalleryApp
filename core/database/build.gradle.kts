plugins {
    alias(libs.plugins.nextgallery.android.library)
    alias(libs.plugins.nextgallery.android.room)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)
}
