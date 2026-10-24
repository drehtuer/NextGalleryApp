plugins {
    alias(libs.plugins.nextgallery.android.library)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)
}
