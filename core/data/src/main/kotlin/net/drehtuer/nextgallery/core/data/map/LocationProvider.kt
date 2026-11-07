package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.GeoPoint

/** Port: the device's position. Implemented with the platform `LocationManager` in `:core:platform` (D44). */
fun interface LocationProvider {
    /** Returns null if the position is unknown or the permission is missing. */
    suspend fun currentLocation(): GeoPoint?
}
