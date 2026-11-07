plugins {
    alias(libs.plugins.nextgallery.jvm.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    api(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    // Android provides the XmlPullParser API at runtime (D25); kxml2 stands in for it in JVM tests.
    compileOnly(libs.xmlpull)

    testImplementation(libs.kxml2)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.tls)
}
