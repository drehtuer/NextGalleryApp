plugins {
    alias(libs.plugins.nextgallery.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    implementation(projects.core.nextcloud)
    api(libs.androidx.paging.common)
    api(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.paging.testing)
}
