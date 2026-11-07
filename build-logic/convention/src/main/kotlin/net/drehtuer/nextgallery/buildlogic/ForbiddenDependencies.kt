package net.drehtuer.nextgallery.buildlogic

/** Proprietary Google libraries must never ship in the app (README, "No Google Play Services dependency"). */
internal object ForbiddenDependencies {
    private val GROUPS = listOf(
        "com.google.android.gms",
        "com.google.firebase",
        "com.google.android.play",
        "com.google.mlkit",
        "com.crashlytics",
    )

    fun isForbidden(group: String): Boolean = GROUPS.any { group == it || group.startsWith("$it.") }

    /** The configurations whose contents end up in the APK; test classpaths don't. */
    fun isShipped(configurationName: String): Boolean =
        configurationName.endsWith("RuntimeClasspath") && "Test" !in configurationName
}
