package net.drehtuer.nextgallery.feature.map

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import io.mockk.mockk
import io.mockk.verify
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.overlay.Marker
import org.robolectric.annotation.GraphicsMode

internal val testMarkerStyle = MarkerStyle(
    markerSizePx = 60,
    borderPx = 3f,
    pillHeightPx = 22f,
    pillTextSizePx = 12f,
    pillPaddingPx = 8f,
    dotSizePx = 20f,
    dotRingPx = 3f,
    haloSizePx = 48,
    accent = Color.BLUE,
    onAccent = Color.WHITE,
    placeholder = Color.GRAY,
    ring = Color.WHITE,
    halo = Color.YELLOW,
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OsmdroidMapRendererTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val imageLoader = mockk<ImageLoader>(relaxed = true)
    private val renderer = OsmdroidMapRenderer(context, testMarkerStyle, imageLoader)
    private val marker = MapMarker(1, GeoPoint(45.5, 10.5), "3", null, "near Bolzano, 3 images")

    private fun markers() = renderer.view.overlays.filterIsInstance<Marker>()

    @Test
    fun setClusters_addsAMarkerWithItsDescription() {
        renderer.setClusters(listOf(marker))

        assertEquals(listOf("near Bolzano, 3 images"), markers().map { it.title })
    }

    @Test
    fun setClusters_sameListAgain_keepsTheMarker() {
        renderer.setClusters(listOf(marker))
        val first = markers().single()

        renderer.setClusters(listOf(marker))

        assertTrue(first === markers().single())
    }

    @Test
    fun setClusters_goneFromList_removesTheMarker() {
        renderer.setClusters(listOf(marker))

        renderer.setClusters(emptyList())

        assertTrue(markers().isEmpty())
    }

    @Test
    fun setClusters_withCover_loadsIt() {
        renderer.setClusters(listOf(marker.copy(cover = MediaThumbnail(1, "content://media/1", 1))))

        verify { imageLoader.enqueue(any()) }
    }

    @Test
    fun setClusters_withoutCover_loadsNothing() {
        renderer.setClusters(listOf(marker))

        verify(exactly = 0) { imageLoader.enqueue(any()) }
    }

    @Test
    fun setCurrentLocation_addsOneDot() {
        renderer.setCurrentLocation(GeoPoint(45.5, 10.5))
        renderer.setCurrentLocation(GeoPoint(46.5, 11.5))

        assertEquals(46.5, markers().single().position.latitude, 0.0)
    }

    @Test
    fun setCurrentLocation_null_removesTheDot() {
        renderer.setCurrentLocation(GeoPoint(45.5, 10.5))

        renderer.setCurrentLocation(null)

        assertTrue(markers().isEmpty())
    }

    @Test
    fun setTileUrl_changesTheTileSource() {
        val url = "https://tiles.example.org/{z}/{x}/{y}.png"

        renderer.setTileUrl(url)

        assertEquals(TemplateTileSource(url).name(), renderer.view.tileProvider.tileSource.name())
    }

    @Test
    fun setCamera_setsTheZoom() {
        renderer.setCamera(MapCamera(GeoPoint(45.5, 10.5), 8.0))

        assertEquals(8.0, renderer.view.zoomLevelDouble, 0.0)
    }

    @Test
    fun viewportOf_valuesInRange_areKept() {
        val bounds = GeoBounds(45.0, 10.0, 46.0, 11.0)

        val viewport = viewportOf(45.5, 10.5, 8.0, bounds)

        assertEquals(GeoPoint(45.5, 10.5) to bounds, viewport.center to viewport.bounds)
    }

    @Test
    fun viewportOf_longitudeBeyondTheAntimeridian_wrapsAround() {
        val viewport = viewportOf(0.0, 190.0, 8.0, GeoBounds(-1.0, -190.0, 1.0, 200.0))

        assertEquals(-170.0, viewport.center.longitude, 1e-9)
        assertEquals(170.0, viewport.bounds.west, 1e-9)
        assertEquals(-160.0, viewport.bounds.east, 1e-9)
    }

    @Test
    fun viewportOf_latitudeBeyondThePole_isClamped() {
        val viewport = viewportOf(95.0, 0.0, 8.0, GeoBounds(-95.0, -1.0, 95.0, 1.0))

        assertEquals(90.0, viewport.center.latitude, 0.0)
        assertEquals(-90.0, viewport.bounds.south, 0.0)
        assertEquals(90.0, viewport.bounds.north, 0.0)
    }
}
