package net.drehtuer.nextgallery.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.access.MediaPermissionSource
import javax.inject.Inject

class AndroidMediaPermissionSource @Inject constructor(@param:ApplicationContext private val context: Context) :
    MediaPermissionSource {
    override fun grantedPermissions(): Set<String> =
        MEDIA_PERMISSIONS.filter { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }.toSet()

    private companion object {
        val MEDIA_PERMISSIONS = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
