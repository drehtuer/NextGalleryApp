package net.drehtuer.nextgallery.feature.folders

import net.drehtuer.nextgallery.core.model.ViewContext
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionOverviewRouteTest {
    @Test
    fun viewContext_bucket_isThatFolder() {
        assertEquals(ViewContext.Folder(7), SelectionOverviewRoute(bucketId = 7).viewContext())
    }

    @Test
    fun viewContext_noBucket_isTimeline() {
        assertEquals(ViewContext.Timeline, SelectionOverviewRoute(bucketId = null).viewContext())
    }

    @Test
    fun viewContext_fromMap_isMap() {
        assertEquals(ViewContext.Map, SelectionOverviewRoute(bucketId = null, fromMap = true).viewContext())
    }
}
