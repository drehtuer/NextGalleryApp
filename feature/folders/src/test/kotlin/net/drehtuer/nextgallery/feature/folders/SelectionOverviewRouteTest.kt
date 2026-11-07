package net.drehtuer.nextgallery.feature.folders

import net.drehtuer.nextgallery.core.model.ViewContext
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionOverviewRouteTest {
    @Test
    fun viewContext_fromFolder_isThatFolder() {
        assertEquals(ViewContext.Folder(7), SelectionOverviewRoute(bucketId = 7).viewContext())
    }

    @Test
    fun viewContext_fromMap_isTheMap() {
        assertEquals(ViewContext.Map, SelectionOverviewRoute(bucketId = 0, fromMap = true).viewContext())
    }
}
