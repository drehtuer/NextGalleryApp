plugins {
    alias(libs.plugins.nextgallery.android.feature)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.osmdroid)
}
