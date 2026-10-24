pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "NextGalleryApp"

include(":app")

include(":core:model")
include(":core:common")
include(":core:data")
include(":core:nextcloud")

include(":core:database")
include(":core:media")
include(":core:platform")
include(":core:ui")

include(":feature:folders")
include(":feature:timeline")
include(":feature:map")
include(":feature:viewer")
include(":feature:editor")
include(":feature:remote")
include(":feature:settings")
