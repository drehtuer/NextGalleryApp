package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.data.access.MediaPermissionSource
import javax.inject.Inject

interface MapPermissions {
    /** `ACCESS_MEDIA_LOCATION`: Android hides the location in photos without it. */
    fun photoLocationsAllowed(): Boolean

    /** `ACCESS_COARSE_LOCATION`: the user's own position (D44). */
    fun currentLocationAllowed(): Boolean
}

class DefaultMapPermissions @Inject constructor(private val permissionSource: MediaPermissionSource) : MapPermissions {
    override fun photoLocationsAllowed(): Boolean = ACCESS_MEDIA_LOCATION in permissionSource.grantedPermissions()

    override fun currentLocationAllowed(): Boolean = ACCESS_COARSE_LOCATION in permissionSource.grantedPermissions()

    private companion object {
        const val ACCESS_MEDIA_LOCATION = "android.permission.ACCESS_MEDIA_LOCATION"
        const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    }
}
