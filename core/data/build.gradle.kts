plugins {
    alias(libs.plugins.nextgallery.jvm.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.nextcloud)
}
