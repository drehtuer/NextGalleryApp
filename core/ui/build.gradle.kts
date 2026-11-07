plugins {
    alias(libs.plugins.nextgallery.android.library)
    alias(libs.plugins.nextgallery.android.compose)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.paging.compose)
    api(libs.coil.compose)
}
