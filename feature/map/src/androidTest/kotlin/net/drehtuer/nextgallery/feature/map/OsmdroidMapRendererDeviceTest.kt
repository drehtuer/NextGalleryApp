package net.drehtuer.nextgallery.feature.map

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.overlay.Marker

/** `MapView` needs a real view hierarchy (ARCHITECTURE.md §13.1). Tiles need a network; the markers do not. */
@RunWith(AndroidJUnit4::class)
class OsmdroidMapRendererDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val style = MarkerStyle(
        markerSizePx = 180,
        borderPx = 9f,
        pillHeightPx = 66f,
        pillTextSizePx = 36f,
        pillPaddingPx = 24f,
        dotSizePx = 60f,
        dotRingPx = 9f,
        haloSizePx = 144,
        accent = Color.BLUE,
        onAccent = Color.WHITE,
        placeholder = Color.GRAY,
        ring = Color.WHITE,
        halo = Color.YELLOW,
    )

    private fun onMainThread(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private fun laidOut(view: View) {
        val parent = FrameLayout(context)
        parent.addView(view, ViewGroup.LayoutParams(SIZE_PX, SIZE_PX))
        val spec = View.MeasureSpec.makeMeasureSpec(SIZE_PX, View.MeasureSpec.EXACTLY)
        parent.measure(spec, spec)
        parent.layout(0, 0, SIZE_PX, SIZE_PX)
    }

    @Test
    fun clusters_appearAsMarkersOnALaidOutMap() {
        onMainThread {
            val renderer = OsmdroidMapRenderer(context, style, ImageLoader(context))
            laidOut(renderer.view)

            renderer.setClusters(listOf(MapMarker(1, GeoPoint(46.5, 11.3), "3", null, "near Bolzano, 3 images")))

            assertEquals(1, renderer.view.overlays.filterIsInstance<Marker>().size)
            renderer.view.onDetach()
        }
    }

    @Test
    fun firstLayout_reportsTheViewport() {
        onMainThread {
            val renderer = OsmdroidMapRenderer(context, style, ImageLoader(context))
            var viewport: Viewport? = null
            renderer.onViewportChanged = { viewport = it }
            renderer.setCamera(MapCamera(GeoPoint(46.5, 11.3), 8.0))

            laidOut(renderer.view)

            assertNotNull(viewport)
            renderer.view.onDetach()
        }
    }

    private companion object {
        const val SIZE_PX = 1000
    }
}
