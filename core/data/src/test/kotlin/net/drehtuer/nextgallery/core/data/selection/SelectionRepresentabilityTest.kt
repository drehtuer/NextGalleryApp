package net.drehtuer.nextgallery.core.data.selection

import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.ViewContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionRepresentabilityTest {
    @Test
    fun canShow_folderWithSameBucket_isTrue() {
        assertTrue(SelectionRepresentability.canShow(mediaItem(1, bucketId = 7), ViewContext.Folder(7)))
    }

    @Test
    fun canShow_folderWithOtherBucket_isFalse() {
        assertFalse(SelectionRepresentability.canShow(mediaItem(1, bucketId = 8), ViewContext.Folder(7)))
    }

    @Test
    fun canShow_timeline_isTrue() {
        assertTrue(SelectionRepresentability.canShow(mediaItem(1), ViewContext.Timeline))
    }

    @Test
    fun canShow_mapWithLocation_isTrue() {
        val item = mediaItem(1).copy(location = GeoPoint(45.6, 10.7))

        assertTrue(SelectionRepresentability.canShow(item, ViewContext.Map))
    }

    @Test
    fun canShow_mapWithoutLocation_isFalse() {
        assertFalse(SelectionRepresentability.canShow(mediaItem(1), ViewContext.Map))
    }
}
