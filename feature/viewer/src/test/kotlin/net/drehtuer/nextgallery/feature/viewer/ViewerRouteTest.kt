package net.drehtuer.nextgallery.feature.viewer

import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerRouteTest {
    @Test
    fun source_noMapBounds_isTheFolder() {
        assertEquals(MediaSource.Folder(7), ViewerRoute(mediaId = 1, bucketId = 7).source())
    }

    @Test
    fun source_mapBounds_isTheCluster() {
        val bounds = GeoBounds(45.0, 10.0, 46.0, 11.0)

        val route = ViewerRoute(mediaId = 1, bucketId = 0, mapBounds = bounds.encode())

        assertEquals(MediaSource.Map(bounds), route.source())
    }

    @Test
    fun source_unreadableMapBounds_fallsBackToTheFolder() {
        val route = ViewerRoute(mediaId = 1, bucketId = 7, mapBounds = "somewhere")

        assertEquals(MediaSource.Folder(7), route.source())
    }
}
