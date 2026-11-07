package net.drehtuer.nextgallery.core.platform

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.nextcloud.ACCESS_LOCAL_NETWORK_PERMISSION
import net.drehtuer.nextgallery.core.data.nextcloud.LocalNetworkPermissionSource
import javax.inject.Inject

class AndroidLocalNetworkPermissionSource @Inject constructor(@param:ApplicationContext private val context: Context) :
    LocalNetworkPermissionSource {
    override fun isGranted(): Boolean =
        context.checkSelfPermission(ACCESS_LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED
}
