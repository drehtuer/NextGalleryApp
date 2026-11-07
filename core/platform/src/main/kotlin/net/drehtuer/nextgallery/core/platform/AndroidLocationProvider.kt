package net.drehtuer.nextgallery.core.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.drehtuer.nextgallery.core.data.map.LocationProvider
import net.drehtuer.nextgallery.core.model.GeoPoint
import javax.inject.Inject
import kotlin.coroutines.resume

/** The platform `LocationManager`, so no Play Services (D44): the last known position, or else one fresh fix. */
class AndroidLocationProvider @Inject constructor(@param:ApplicationContext private val context: Context) :
    LocationProvider {
    override suspend fun currentLocation(): GeoPoint? {
        val manager = context.getSystemService(LocationManager::class.java)
        return try {
            (lastKnown(manager) ?: freshFix(manager))?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission") // The caller checks; a missing permission ends in the catch above.
    private fun lastKnown(manager: LocationManager): Location? =
        manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }

    @SuppressLint("MissingPermission") // See lastKnown.
    private suspend fun freshFix(manager: LocationManager): Location? {
        val provider = manager.getProviders(true).firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return null
        return withTimeoutOrNull(FIX_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { continuation.resume(it) }
            }
        }
    }

    private companion object {
        const val FIX_TIMEOUT_MILLIS = 5_000L
    }
}
