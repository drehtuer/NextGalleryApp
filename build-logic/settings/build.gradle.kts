// Settings plugins get their own jar: Gradle loads them into the settings class loader, which does not see the Android
// Gradle plugin the convention plugins are built against.
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
}

group = "net.drehtuer.nextgallery.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

// Lets Gradle's Action<T> lambdas use `this`, as in build scripts.
samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

dependencies {
    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        register("dependencyFloors") {
            id = "nextgallery.dependency-floors"
            implementationClass = "DependencyFloorsSettingsPlugin"
        }
    }
}
