package net.drehtuer.nextgallery.feature.viewer

import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerRouteTest {
    private val bounds = GeoBounds(45.0, 10.0, 46.0, 11.0)

    @Test
    fun source_bucket_isThatFolder() {
        assertEquals(MediaSource.Folder(7), ViewerRoute(mediaId = 1, bucketId = 7).source())
    }

    @Test
    fun source_noBucket_isTimeline() {
        assertEquals(MediaSource.Timeline, ViewerRoute(mediaId = 1, bucketId = null).source())
    }

    @Test
    fun source_mapBounds_isThatCluster() {
        val route = ViewerRoute(mediaId = 1, bucketId = null, mapBounds = bounds.encode())

        assertEquals(MediaSource.Map(bounds), route.source())
    }

    @Test
    fun source_invalidMapBounds_fallsBackToTimeline() {
        val route = ViewerRoute(mediaId = 1, bucketId = null, mapBounds = "invalid")

        assertEquals(MediaSource.Timeline, route.source())
    }
}
