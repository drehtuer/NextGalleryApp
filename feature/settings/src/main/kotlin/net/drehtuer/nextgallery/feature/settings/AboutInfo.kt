package net.drehtuer.nextgallery.feature.settings

import androidx.annotation.DrawableRes

/** What only the app module knows: `BuildConfig` and the launcher icon's two layers. */
data class AboutInfo(
    val versionName: String,
    val buildNumber: Int,
    @param:DrawableRes val iconBackground: Int,
    @param:DrawableRes val iconForeground: Int,
)
