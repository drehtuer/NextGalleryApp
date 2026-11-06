package net.drehtuer.nextgallery.core.data.access

import net.drehtuer.nextgallery.core.model.MediaAccess
import javax.inject.Inject

interface MediaAccessRepository {
    /** Permissions to request together, so the system offers both "allow all" and "select photos". */
    val requestedPermissions: List<String>

    fun currentAccess(): MediaAccess
}

class DefaultMediaAccessRepository @Inject constructor(private val permissionSource: MediaPermissionSource) :
    MediaAccessRepository {
    override val requestedPermissions: List<String> = listOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED)

    override fun currentAccess(): MediaAccess {
        val granted = permissionSource.grantedPermissions()
        return when {
            READ_MEDIA_IMAGES in granted -> MediaAccess.FULL
            READ_MEDIA_VISUAL_USER_SELECTED in granted -> MediaAccess.PARTIAL
            else -> MediaAccess.DENIED
        }
    }

    companion object {
        const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
        const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    }
}
