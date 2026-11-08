// Plain Kotlin instead of `kotlin-dsl`: that plugin compiles with the Kotlin embedded in Gradle (2.4.10, affected by
// GHSA-r937-wjx7-w2jp), this one with the project's Kotlin from the version catalog (D62).
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
}

group = "net.drehtuer.nextgallery.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

// Lets Gradle's Action<T> lambdas use `this`, as in build scripts; `kotlin-dsl` did this before.
samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)

    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "nextgallery.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "nextgallery.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidBenchmark") {
            id = "nextgallery.android.benchmark"
            implementationClass = "AndroidBenchmarkConventionPlugin"
        }
        register("androidCompose") {
            id = "nextgallery.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "nextgallery.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "nextgallery.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("androidFeature") {
            id = "nextgallery.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidTest") {
            id = "nextgallery.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("jvmLibrary") {
            id = "nextgallery.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("detekt") {
            id = "nextgallery.detekt"
            implementationClass = "DetektConventionPlugin"
        }
        register("ktlint") {
            id = "nextgallery.ktlint"
            implementationClass = "KtlintConventionPlugin"
        }
    }
}
