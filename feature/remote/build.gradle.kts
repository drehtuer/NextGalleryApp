plugins {
    alias(libs.plugins.nextgallery.android.feature)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.telephoto.zoomable.image.coil)
}
