package net.drehtuer.nextgallery.feature.map

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.data.index.IndexStatus
import net.drehtuer.nextgallery.core.data.map.LocationProvider
import net.drehtuer.nextgallery.core.data.map.MapPermissions
import net.drehtuer.nextgallery.core.data.map.MapRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.Viewport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val home = GeoPoint(45.5, 10.5)
    private val startCamera = MapCamera(GeoPoint(1.0, 2.0), 8.0)
    private val clusters = MutableStateFlow<List<MapCluster>>(emptyList())
    private val hasGeotagged = MutableStateFlow(true)
    private val indexStatus = MutableStateFlow<IndexStatus>(IndexStatus.Idle)
    private val selected = MutableStateFlow<Set<MediaId>>(emptySet())
    private val settings = MutableStateFlow(UserSettings())
    private var photoLocationsAllowed = true
    private var currentLocationAllowed = true

    private val mapRepository = mockk<MapRepository> {
        every { observeClusters(any()) } returns clusters
        every { observeHasGeotagged() } returns hasGeotagged
        coEvery { startCamera(any()) } returns startCamera
    }
    private val mediaRepository = mockk<MediaRepository> { every { pagedItems(any()) } returns emptyFlow() }
    private val settingsRepository = mockk<SettingsRepository> {
        every { observeSettings() } returns settings
        coEvery { setCurrentLocationAsked() } returns AppResult.Success(Unit)
    }
    private val permissions = mockk<MapPermissions> {
        every { photoLocationsAllowed() } answers { photoLocationsAllowed }
        every { currentLocationAllowed() } answers { currentLocationAllowed }
    }
    private val locationProvider = mockk<LocationProvider> { coEvery { currentLocation() } returns home }
    private val indexScheduler = mockk<IndexScheduler>(relaxed = true)
    private val indexRepository = mockk<IndexRepository> { every { status } returns indexStatus }
    private val selection = mockk<MapSelectionActions> {
        every { observe() } returns selected
        coEvery { handle(any(), any()) } returns null
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MapViewModel(
        mapRepository,
        mediaRepository,
        settingsRepository,
        permissions,
        locationProvider,
        indexScheduler,
        indexRepository,
        selection,
    )

    /** Keeps the state flow running, as the screen does. */
    private fun TestScope.subscribed() = viewModel().also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun open_locationAllowed_startsAtCurrentLocation() = runTest {
        viewModel()

        coVerify { mapRepository.startCamera(home) }
    }

    @Test
    fun open_locationAllowed_movesCameraThere() = runTest {
        viewModel().effects.test {
            assertEquals(MapEffect.MoveCamera(startCamera), awaitItem())
        }
    }

    @Test
    fun open_locationAllowed_showsLocationDot() = runTest {
        assertEquals(home, subscribed().state.value.currentLocation)
    }

    @Test
    fun open_firstTimeWithoutPermission_asksForIt() = runTest {
        currentLocationAllowed = false

        viewModel().effects.test {
            assertEquals(MapEffect.RequestCurrentLocationPermission, awaitItem())
        }
    }

    @Test
    fun open_firstTimeWithoutPermission_remembersThatItAsked() = runTest {
        currentLocationAllowed = false

        viewModel()

        coVerify { settingsRepository.setCurrentLocationAsked() }
    }

    @Test
    fun open_alreadyAskedAndDenied_startsWithoutLocation() = runTest {
        currentLocationAllowed = false
        settings.value = UserSettings(currentLocationAsked = true)

        viewModel()

        coVerify { mapRepository.startCamera(null) }
    }

    @Test
    fun locationPromptClosed_granted_startsAtCurrentLocation() = runTest {
        currentLocationAllowed = false
        val viewModel = viewModel()
        currentLocationAllowed = true

        viewModel.onEvent(MapEvent.CurrentLocationPromptClosed)

        coVerify { mapRepository.startCamera(home) }
    }

    @Test
    fun locationPromptClosed_denied_startsOnAreaWithMostImages() = runTest {
        currentLocationAllowed = false
        val viewModel = viewModel()

        viewModel.onEvent(MapEvent.CurrentLocationPromptClosed)

        coVerify { mapRepository.startCamera(null) }
    }

    @Test
    fun viewportChanged_clustersFollowIt() = runTest {
        val cluster = mapCluster()
        every { mapRepository.observeClusters(any()) } answers {
            firstArg<Flow<Viewport>>().map { viewport ->
                if (viewport ==
                    testViewport
                ) {
                    listOf(cluster)
                } else {
                    emptyList()
                }
            }
        }
        val viewModel = subscribed()

        viewModel.onEvent(MapEvent.ViewportChanged(testViewport))

        assertEquals(listOf(cluster), viewModel.state.value.clusters)
    }

    @Test
    fun viewportChanged_isWhereANewMapStarts() = runTest {
        val viewModel = subscribed()

        viewModel.onEvent(MapEvent.ViewportChanged(testViewport))

        assertEquals(MapCamera(testViewport.center, testViewport.zoom), viewModel.state.value.camera)
    }

    @Test
    fun zoomIn_movesCameraOneLevelIn() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(MapEvent.ViewportChanged(testViewport))

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ZoomIn)
            assertEquals(MapEffect.MoveCamera(MapCamera(testViewport.center, 11.0)), awaitItem())
        }
    }

    @Test
    fun zoomOut_movesCameraOneLevelOut() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(MapEvent.ViewportChanged(testViewport))

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ZoomOut)
            assertEquals(MapEffect.MoveCamera(MapCamera(testViewport.center, 9.0)), awaitItem())
        }
    }

    @Test
    fun zoomIn_atMaximum_staysAt19() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(MapEvent.ViewportChanged(testViewport.copy(zoom = 19.0)))

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ZoomIn)
            assertEquals(MapEffect.MoveCamera(MapCamera(testViewport.center, 19.0)), awaitItem())
        }
    }

    @Test
    fun zoomOut_atMinimum_staysAt0() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(MapEvent.ViewportChanged(testViewport.copy(zoom = 0.0)))

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ZoomOut)
            assertEquals(MapEffect.MoveCamera(MapCamera(testViewport.center, 0.0)), awaitItem())
        }
    }

    @Test
    fun zoomIn_viewportNotKnownYet_doesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ZoomIn)
            expectNoEvents()
        }
    }

    @Test
    fun showMyLocation_allowed_movesToItAtZoom8() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ShowMyLocation)
            assertEquals(MapEffect.MoveCamera(MapCamera(home, 8.0)), awaitItem())
        }
    }

    @Test
    fun showMyLocation_positionUnknown_tellsTheUser() = runTest {
        val viewModel = viewModel()
        coEvery { locationProvider.currentLocation() } returns null

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ShowMyLocation)
            assertEquals(MapEffect.LocationUnavailable, awaitItem())
        }
    }

    @Test
    fun showMyLocation_notAllowed_asksForPermission() = runTest {
        settings.value = UserSettings(currentLocationAsked = true)
        currentLocationAllowed = false
        val viewModel = viewModel()

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ShowMyLocation)
            assertEquals(MapEffect.RequestCurrentLocationPermission, awaitItem())
        }
    }

    @Test
    fun showMyLocation_promptGranted_movesToIt() = runTest {
        settings.value = UserSettings(currentLocationAsked = true)
        currentLocationAllowed = false
        val viewModel = viewModel()

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.ShowMyLocation)
            skipItems(1)
            currentLocationAllowed = true
            viewModel.onEvent(MapEvent.CurrentLocationPromptClosed)
            assertEquals(MapEffect.MoveCamera(MapCamera(home, 8.0)), awaitItem())
        }
    }

    @Test
    fun clusterClicked_opensItsSheet() = runTest {
        val cluster = mapCluster(count = 3)
        clusters.value = listOf(cluster)
        val viewModel = subscribed()

        viewModel.onEvent(MapEvent.ClusterClicked(key = 3))

        assertEquals(cluster, viewModel.state.value.openCluster)
    }

    @Test
    fun clusterClicked_unknownKey_opensNothing() = runTest {
        clusters.value = listOf(mapCluster(count = 3))
        val viewModel = subscribed()

        viewModel.onEvent(MapEvent.ClusterClicked(key = 99))

        assertNull(viewModel.state.value.openCluster)
    }

    @Test
    fun clusterClosed_closesTheSheet() = runTest {
        clusters.value = listOf(mapCluster(count = 3))
        val viewModel = subscribed()
        viewModel.onEvent(MapEvent.ClusterClicked(key = 3))

        viewModel.onEvent(MapEvent.ClusterClosed)

        assertNull(viewModel.state.value.openCluster)
    }

    @Test
    fun sheetItems_openCluster_pagesThroughIt() = runTest {
        val cluster = mapCluster(count = 3)
        clusters.value = listOf(cluster)
        val viewModel = subscribed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sheetItems.collect {} }

        viewModel.onEvent(MapEvent.ClusterClicked(key = 3))

        verify { mediaRepository.pagedItems(MediaSource.Map(cluster.cluster.bounds)) }
    }

    @Test
    fun sheetItems_noOpenCluster_readsNothing() = runTest {
        val viewModel = viewModel()

        viewModel.sheetItems.first()

        verify(exactly = 0) { mediaRepository.pagedItems(any()) }
    }

    @Test
    fun selectionEvent_passesOpenClusterBounds() = runTest {
        val cluster = mapCluster(count = 3)
        clusters.value = listOf(cluster)
        val viewModel = subscribed()
        viewModel.onEvent(MapEvent.ClusterClicked(key = 3))

        viewModel.onEvent(MapSelectionEvent.Clear)

        coVerify { selection.handle(MapSelectionEvent.Clear, cluster.cluster.bounds) }
    }

    @Test
    fun selectionEvent_effectReachesTheScreen() = runTest {
        val effect = MapEffect.LocationUnavailable
        coEvery { selection.handle(MapSelectionEvent.Share, null) } returns effect
        val viewModel = viewModel()

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapSelectionEvent.Share)
            assertEquals(effect, awaitItem())
        }
    }

    @Test
    fun selectedIds_comeFromTheSelection() = runTest {
        selected.value = setOf(MediaId(1))

        assertEquals(setOf(MediaId(1)), subscribed().state.value.selectedIds)
    }

    @Test
    fun tileUrl_comesFromSettings() = runTest {
        settings.value = UserSettings(tileUrl = "https://tiles.example.org/{z}/{x}/{y}.png")

        assertEquals("https://tiles.example.org/{z}/{x}/{y}.png", subscribed().state.value.tileUrl)
    }

    @Test
    fun photoLocationCard_notAllowed_isShown() = runTest {
        photoLocationsAllowed = false

        assertTrue(subscribed().state.value.showPhotoLocationCard)
    }

    @Test
    fun photoLocationCard_allowed_isHidden() = runTest {
        assertFalse(subscribed().state.value.showPhotoLocationCard)
    }

    @Test
    fun photoLocationCard_dismissed_isHidden() = runTest {
        photoLocationsAllowed = false
        val viewModel = subscribed()

        viewModel.onEvent(MapEvent.DismissPhotoLocations)

        assertFalse(viewModel.state.value.showPhotoLocationCard)
    }

    @Test
    fun allowPhotoLocations_asksForPermission() = runTest {
        photoLocationsAllowed = false
        val viewModel = viewModel()

        viewModel.effects.test {
            skipItems(1)
            viewModel.onEvent(MapEvent.AllowPhotoLocations)
            assertEquals(MapEffect.RequestPhotoLocationPermission, awaitItem())
        }
    }

    @Test
    fun permissionsChanged_photoLocationsNewlyAllowed_hidesCard() = runTest {
        photoLocationsAllowed = false
        val viewModel = subscribed()
        photoLocationsAllowed = true

        viewModel.onEvent(MapEvent.PermissionsChanged)

        assertFalse(viewModel.state.value.showPhotoLocationCard)
    }

    @Test
    fun permissionsChanged_photoLocationsNewlyAllowed_startsTheLocationPass() = runTest {
        photoLocationsAllowed = false
        val viewModel = viewModel()
        photoLocationsAllowed = true

        viewModel.onEvent(MapEvent.PermissionsChanged)

        verify { indexScheduler.onPhotoLocationsAllowed() }
    }

    @Test
    fun permissionsChanged_alreadyAllowed_doesNotIndexAgain() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(MapEvent.PermissionsChanged)

        verify(exactly = 0) { indexScheduler.onPhotoLocationsAllowed() }
    }

    @Test
    fun noGeotaggedCard_allowedAndNoImages_isShown() = runTest {
        hasGeotagged.value = false

        assertTrue(subscribed().state.value.showNoGeotaggedCard)
    }

    @Test
    fun noGeotaggedCard_hasImages_isHidden() = runTest {
        assertFalse(subscribed().state.value.showNoGeotaggedCard)
    }

    @Test
    fun noGeotaggedCard_whileIndexing_isHidden() = runTest {
        hasGeotagged.value = false
        indexStatus.value = IndexStatus.Running

        assertFalse(subscribed().state.value.showNoGeotaggedCard)
    }

    @Test
    fun noGeotaggedCard_notAllowed_isHiddenBecauseItWouldBeWrong() = runTest {
        hasGeotagged.value = false
        photoLocationsAllowed = false

        assertFalse(subscribed().state.value.showNoGeotaggedCard)
    }
}
