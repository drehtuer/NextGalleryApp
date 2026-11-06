plugins {
    alias(libs.plugins.nextgallery.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    implementation(projects.core.nextcloud)
    api(libs.androidx.paging.common)

    testImplementation(libs.androidx.paging.testing)
}
