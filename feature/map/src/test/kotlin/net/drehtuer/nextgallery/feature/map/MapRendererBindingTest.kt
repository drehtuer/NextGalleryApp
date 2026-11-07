package net.drehtuer.nextgallery.feature.map

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapRendererBindingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val renderer = FakeMapRenderer()
    private val events = mutableListOf<MapEvent>()

    private fun show(state: MapUiState) = composeRule.setContent {
        MapRendererBinding(renderer, state) { events += it }
    }

    @Test
    fun clusters_becomeMarkers() {
        show(MapUiState(clusters = listOf(mapCluster(3))))

        composeRule.runOnIdle { assertEquals(listOf(3L), renderer.markers.map { it.key }) }
    }

    @Test
    fun camera_startsAtTheStateCamera() {
        val camera = MapCamera(GeoPoint(1.0, 2.0), 5.0)

        show(MapUiState(camera = camera))

        composeRule.runOnIdle { assertEquals(camera, renderer.currentCamera) }
    }

    @Test
    fun currentLocation_drawsTheDot() {
        val location = GeoPoint(1.0, 2.0)

        show(MapUiState(currentLocation = location))

        composeRule.runOnIdle { assertEquals(location, renderer.location) }
    }

    @Test
    fun tileUrl_goesToTheRenderer() {
        show(MapUiState(tileUrl = "https://tiles.example.org/{z}/{x}/{y}.png"))

        composeRule.runOnIdle { assertEquals("https://tiles.example.org/{z}/{x}/{y}.png", renderer.currentTileUrl) }
    }

    @Test
    fun viewportChange_becomesEvent() {
        show(MapUiState())

        composeRule.runOnIdle { renderer.onViewportChanged?.invoke(testViewport) }

        assertEquals(listOf<MapEvent>(MapEvent.ViewportChanged(testViewport)), events)
    }

    @Test
    fun clusterClick_becomesEvent() {
        show(MapUiState())

        composeRule.runOnIdle { renderer.onClusterClick?.invoke(3) }

        assertEquals(listOf<MapEvent>(MapEvent.ClusterClicked(3)), events)
    }
}
