plugins {
    alias(libs.plugins.nextgallery.jvm.library)
}

dependencies {
    implementation(projects.core.model)
    api(libs.kotlinx.coroutines.core)
}
