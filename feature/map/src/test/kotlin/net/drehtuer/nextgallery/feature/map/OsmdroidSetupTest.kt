package net.drehtuer.nextgallery.feature.map

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.File

@RunWith(AndroidJUnit4::class)
class OsmdroidSetupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val configuration = Configuration.getInstance()

    @Before
    fun setUp() = OsmdroidSetup.configure(configuration, context.cacheDir)

    private fun configuredView() = MapView(context).also(OsmdroidSetup::configure)

    @Test
    fun configuration_userAgent_namesTheAppWithoutPersonalData() {
        assertEquals("NextGalleryApp (Android, osmdroid)", configuration.userAgentValue)
    }

    @Test
    fun configuration_tileCache_isInTheCacheFolder() {
        assertEquals(File(context.cacheDir, "osmdroid/tiles"), configuration.osmdroidTileCache)
    }

    @Test
    fun configuration_basePath_isInTheCacheFolder() {
        assertEquals(File(context.cacheDir, "osmdroid"), configuration.osmdroidBasePath)
    }

    @Test
    fun view_zoomsFromTheGlobeToOpenStreetMapsMaximum() {
        val view = configuredView()

        assertEquals(0.0, view.minZoomLevel, 0.0)
        assertEquals(19.0, view.maxZoomLevel, 0.0)
    }

    @Test
    fun view_worldRepeatsHorizontallyOnly() {
        val view = configuredView()

        assertTrue(view.isHorizontalMapRepetitionEnabled)
        assertFalse(view.isVerticalMapRepetitionEnabled)
    }

    @Test
    fun tileSource_fillsInThePlaceholders() {
        val source = TemplateTileSource("https://tiles.example.org/{z}/{x}/{y}.png")

        val url = source.getTileURLString(MapTileIndex.getTileIndex(5, 16, 11))

        assertEquals("https://tiles.example.org/5/16/11.png", url)
    }

    @Test
    fun tileSource_zoomRangeMatchesTheMap() {
        val source = TemplateTileSource("https://tiles.example.org/{z}/{x}/{y}.png")

        assertEquals(0 to 19, source.minimumZoomLevel to source.maximumZoomLevel)
    }

    @Test
    fun tileSource_differentServers_doNotShareACache() {
        val first = TemplateTileSource("https://one.example.org/{z}/{x}/{y}.png")
        val second = TemplateTileSource("https://two.example.org/{z}/{x}/{y}.png")

        assertTrue(first.name() != second.name())
    }
}
