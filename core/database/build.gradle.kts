plugins {
    alias(libs.plugins.nextgallery.android.library)
    alias(libs.plugins.nextgallery.android.room)
    alias(libs.plugins.nextgallery.android.hilt)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)

    testImplementation(libs.androidx.paging.testing)
}
