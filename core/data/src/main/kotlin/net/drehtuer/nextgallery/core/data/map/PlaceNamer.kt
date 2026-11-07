package net.drehtuer.nextgallery.core.data.map

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.model.Cluster
import net.drehtuer.nextgallery.core.model.ClusterTitle
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.Place
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Port: the bundled place list. Empty if the app ships without it; everything then shows coordinates only. */
fun interface PlaceSource {
    suspend fun load(): List<Place>
}

/** Offline reverse lookup (D45). One instance, so the list is read once; lookups are kept, because maps repeat them. */
@Singleton
class PlaceNamer @Inject constructor(private val source: PlaceSource, private val dispatchers: DispatcherProvider) {
    private val mutex = Mutex()
    private var index: PlaceIndex? = null
    private val lookups = ConcurrentHashMap<GeoPoint, Optional<Place>>()

    /** The nearest place within 30 km, or null. */
    suspend fun nearest(point: GeoPoint): Place? {
        lookups[point]?.let { return it.orElse(null) }
        val place = index().nearest(point)
        lookups[point] = Optional.ofNullable(place)
        return place
    }

    /** Named after the largest place near any of its images; null if none has a place. */
    suspend fun clusterTitle(cluster: Cluster): ClusterTitle? {
        val places = cluster.items.mapNotNullTo(HashSet()) { nearest(it.location) }
        val largest = places.maxByOrNull { it.population } ?: return null
        return ClusterTitle(largest, places.size - 1)
    }

    private suspend fun index(): PlaceIndex = mutex.withLock {
        index ?: withContext(dispatchers.io) { PlaceIndex(source.load()) }.also { index = it }
    }
}
