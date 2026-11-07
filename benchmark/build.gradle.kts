plugins {
    alias(libs.plugins.nextgallery.android.benchmark)
    alias(libs.plugins.baselineprofile)
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
