package net.drehtuer.nextgallery.core.model

/**
 * A folder the user added with the system's folder picker (D55). Its images and those of its subfolders are shown.
 * [volumeName] is a MediaStore volume, [relativePath] has no leading or trailing slash.
 */
data class AddedFolder(val volumeName: String, val relativePath: String) {
    val name: String get() = relativePath.substringAfterLast('/')

    /** Is it on the phone's own storage, rather than on an SD card or a USB drive? */
    val isOnPrimaryVolume: Boolean get() = volumeName == PRIMARY_VOLUME

    companion object {
        /** The value of `MediaStore.VOLUME_EXTERNAL_PRIMARY`. */
        const val PRIMARY_VOLUME = "external_primary"
    }
}
