package net.drehtuer.nextgallery.feature.map

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.ClusterTitle
import net.drehtuer.nextgallery.core.ui.thumbnail.toThumbnail
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClusterTextTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun countLabel_upTo999_isTheNumber() {
        assertEquals("999", clusterCountLabel(999))
    }

    @Test
    fun countLabel_above999_isCapped() {
        assertEquals("999+", clusterCountLabel(1_000))
    }

    @Test
    fun description_withTown_namesIt() {
        assertEquals("near Bolzano, 128 images", clusterDescription(resources, mapCluster(128)))
    }

    @Test
    fun description_oneImage_isSingular() {
        assertEquals("near Bolzano, 1 image", clusterDescription(resources, mapCluster(1)))
    }

    @Test
    fun description_withoutTown_isTheCount() {
        assertEquals("4 images", clusterDescription(resources, mapCluster(4, title = null)))
    }

    @Test
    fun sheetTitle_onePlace_isNearTheTown() {
        assertEquals("Near Bolzano", clusterSheetTitle(resources, mapCluster(128)))
    }

    @Test
    fun sheetTitle_mergedCluster_countsTheOtherPlaces() {
        val cluster = mapCluster(128, ClusterTitle(bolzano, morePlaces = 7))

        assertEquals("Near Bolzano and 7 more places", clusterSheetTitle(resources, cluster))
    }

    @Test
    fun sheetTitle_oneMorePlace_isSingular() {
        val cluster = mapCluster(128, ClusterTitle(bolzano, morePlaces = 1))

        assertEquals("Near Bolzano and 1 more place", clusterSheetTitle(resources, cluster))
    }

    @Test
    fun sheetTitle_withoutTown_isTheCount() {
        assertEquals("4 images", clusterSheetTitle(resources, mapCluster(4, title = null)))
    }

    @Test
    fun sheetSubtitle_withTown_isTheCount() {
        assertEquals("128 images", clusterSheetSubtitle(resources, mapCluster(128)))
    }

    @Test
    fun sheetSubtitle_withoutTown_isTheCoordinates() {
        assertEquals("46.50° N, 11.30° E", clusterSheetSubtitle(resources, mapCluster(4, title = null)))
    }

    @Test
    fun toMarker_carriesWhatTheMapDraws() {
        val cluster = mapCluster(1_200)

        val marker = cluster.toMarker(resources)

        assertEquals(
            MapMarker(
                1_200,
                cluster.cluster.center,
                "999+",
                mediaItem(1_200).toThumbnail(),
                "near Bolzano, 1,200 images",
            ),
            marker,
        )
    }
}
