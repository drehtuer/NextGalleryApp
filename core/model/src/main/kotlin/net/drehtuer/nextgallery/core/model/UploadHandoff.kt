package net.drehtuer.nextgallery.core.model

/** The official Nextcloud Android app, which does all uploading (D31). */
const val NEXTCLOUD_APP_PACKAGE = "com.nextcloud.client"

/** How the Upload action is shown (D31, D35). */
enum class UploadAvailability {
    /** Logged out: no Upload action at all. */
    HIDDEN,

    /** Drawn dimmed; tapping it explains that the Nextcloud app is needed. */
    APP_MISSING,

    AVAILABLE,
}

/** The outcome of asking to upload some images. `:core:ui` carries it out. */
sealed interface UploadHandoff {
    /** Send [spec] to the Nextcloud app. */
    data class Ready(val spec: ShareSpec) : UploadHandoff

    data object NothingToUpload : UploadHandoff

    data object NextcloudAppMissing : UploadHandoff
}
