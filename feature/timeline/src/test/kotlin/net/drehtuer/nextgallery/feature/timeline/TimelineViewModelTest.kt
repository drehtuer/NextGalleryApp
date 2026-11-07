package net.drehtuer.nextgallery.feature.timeline

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.data.timeline.TimelineIndex
import net.drehtuer.nextgallery.core.data.timeline.TimelineRepository
import net.drehtuer.nextgallery.core.data.upload.UploadHandoffRepository
import net.drehtuer.nextgallery.core.data.upload.UploadStateRepository
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadHandoff
import net.drehtuer.nextgallery.core.model.UploadState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {
    private val index = TimelineIndex(listOf(DayCount(LocalDate.of(2024, 3, 16), 2)))
    private val selectedIds = MutableStateFlow<Set<MediaId>>(emptySet())
    private val mediaRepository = mockk<MediaRepository> {
        every { pagedItems(MediaSource.Timeline) } returns emptyFlow()
        coEvery { timelineItemIds() } returns listOf(MediaId(1), MediaId(2))
    }
    private val timelineRepository = mockk<TimelineRepository> {
        every { observeIndex() } returns flowOf(index)
    }
    private val selectionRepository = mockk<SelectionRepository>(relaxed = true) {
        every { observe() } returns selectedIds
    }
    private val spec = ShareSpec(listOf("content://media/1"), "image/jpeg")
    private val actions = mockk<MediaActionsRepository> {
        coEvery { shareSpec(setOf(MediaId(1))) } returns spec
    }
    private val trashFlow = mockk<TrashFlow>(relaxed = true)
    private val visibleIds = slot<Flow<Set<MediaId>>>()
    private val badges = MutableStateFlow<Map<MediaId, UploadState>>(emptyMap())
    private val uploadStates = mockk<UploadStateRepository> {
        every { observeBadges(capture(visibleIds)) } returns badges
    }
    private val uploadHandoff = mockk<UploadHandoffRepository>(relaxed = true) {
        every { observeAvailability() } returns flowOf(UploadAvailability.APP_MISSING)
    }

    /** 6 November 2026, 23:30 UTC, which is already the 7th at UTC+1. */
    private val clock = mockk<Clock> {
        every { nowMillis() } returns 1_794_007_800_000L
        every { zone() } returns ZoneOffset.ofHours(1)
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TimelineViewModel(
        mediaRepository,
        timelineRepository,
        selectionRepository,
        actions,
        trashFlow,
        clock,
        uploadStates,
        uploadHandoff,
    )

    private suspend fun TimelineViewModel.loadedState() = state.first { it.index != null }

    @Test
    fun state_containsUploadBadges() = runTest {
        badges.value = mapOf(MediaId(1) to UploadState.NotUploaded)

        val state = viewModel().state.first { it.uploadBadges.isNotEmpty() }

        assertEquals(mapOf(MediaId(1) to UploadState.NotUploaded), state.uploadBadges)
    }

    @Test
    fun state_containsUploadAvailability() = runTest {
        assertEquals(UploadAvailability.APP_MISSING, viewModel().loadedState().uploadAvailability)
    }

    @Test
    fun onEvent_visibleItemsChanged_reachesTheBadges() = runTest {
        val viewModel = viewModel()
        viewModel.loadedState()

        viewModel.onEvent(TimelineEvent.VisibleItemsChanged(setOf(MediaId(3))))

        assertEquals(setOf(MediaId(3)), visibleIds.captured.first())
    }

    @Test
    fun onEvent_uploadSelected_sendsTheHandoffOfTheSelection() = runTest {
        selectedIds.value = setOf(MediaId(1))
        coEvery { uploadHandoff.prepare(setOf(MediaId(1))) } returns UploadHandoff.NothingToUpload
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.UploadSelected)

        viewModel.effects.test {
            assertEquals(TimelineEffect.Upload(UploadHandoff.NothingToUpload), awaitItem())
            cancel()
        }
    }

    @Test
    fun onEvent_resumed_tellsTheRepository() {
        viewModel().onEvent(TimelineEvent.Resumed)

        verify { uploadHandoff.onAppResumed() }
    }

    @Test
    fun init_pagesThroughTimeline() {
        viewModel()

        verify { mediaRepository.pagedItems(MediaSource.Timeline) }
    }

    @Test
    fun state_beforeLoading_hasNoIndex() {
        assertEquals(null, viewModel().state.value.index)
    }

    @Test
    fun state_containsIndex() = runTest {
        assertEquals(index, viewModel().loadedState().index)
    }

    @Test
    fun state_todayUsesDeviceTimeZone() = runTest {
        assertEquals(LocalDate.of(2026, 11, 7), viewModel().loadedState().today)
    }

    @Test
    fun state_containsSelection() = runTest {
        selectedIds.value = setOf(MediaId(1))

        assertEquals(setOf(MediaId(1)), viewModel().loadedState().selectedIds)
    }

    @Test
    fun onEvent_itemClickedWithoutSelection_opensViewer() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.ItemClicked(MediaId(1)))

        viewModel.effects.test { assertEquals(TimelineEffect.OpenViewer(MediaId(1)), awaitItem()) }
    }

    @Test
    fun onEvent_itemClickedDuringSelection_togglesItem() {
        selectedIds.value = setOf(MediaId(9))

        viewModel().onEvent(TimelineEvent.ItemClicked(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_startSelecting_setsSelecting() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.StartSelecting)

        assertTrue(viewModel.loadedState().selecting)
    }

    @Test
    fun onEvent_itemClickedAfterStartSelecting_togglesItem() {
        val viewModel = viewModel()
        viewModel.onEvent(TimelineEvent.StartSelecting)

        viewModel.onEvent(TimelineEvent.ItemClicked(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_itemLongPressed_startsSelectionWithItem() {
        viewModel().onEvent(TimelineEvent.ItemLongPressed(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_selectAll_selectsAllImagesOfTimeline() {
        viewModel().onEvent(TimelineEvent.SelectAll)

        verify { selectionRepository.selectAll(listOf(MediaId(1), MediaId(2))) }
    }

    @Test
    fun onEvent_clearSelection_stopsSelecting() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(TimelineEvent.StartSelecting)

        viewModel.onEvent(TimelineEvent.ClearSelection)

        assertFalse(viewModel.loadedState().selecting)
    }

    @Test
    fun onEvent_clearSelection_clearsRepository() {
        viewModel().onEvent(TimelineEvent.ClearSelection)

        verify { selectionRepository.clear() }
    }

    @Test
    fun onEvent_shareSelected_sendsShareEffect() = runTest {
        selectedIds.value = setOf(MediaId(1))
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.ShareSelected)

        viewModel.effects.test { assertEquals(TimelineEffect.Share(spec), awaitItem()) }
    }

    @Test
    fun onEvent_shareSelectedWithNothingToShare_sendsNoEffect() = runTest {
        coEvery { actions.shareSpec(emptySet()) } returns null
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.ShareSelected)

        viewModel.effects.test { expectNoEvents() }
    }

    @Test
    fun onEvent_deleteSelected_requestsTrashForWholeSelection() = runTest {
        selectedIds.value = setOf(MediaId(1), MediaId(9))
        coEvery { trashFlow.request(setOf(MediaId(1), MediaId(9))) } returns TrashEffect.Failed
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.DeleteSelected)

        viewModel.effects.test { assertEquals(TimelineEffect.Trash(TrashEffect.Failed), awaitItem()) }
    }

    @Test
    fun onEvent_deleteSelectedWithNothingToDelete_sendsNoEffect() = runTest {
        coEvery { trashFlow.request(any()) } returns null
        val viewModel = viewModel()

        viewModel.onEvent(TimelineEvent.DeleteSelected)

        viewModel.effects.test { expectNoEvents() }
    }

    @Test
    fun onEvent_confirmationResult_goesToTrashFlow() {
        viewModel().onEvent(TimelineEvent.ConfirmationResult(confirmed = true))

        coVerify { trashFlow.onConfirmationResult(true) }
    }

    @Test
    fun onEvent_undoDelete_goesToTrashFlow() {
        viewModel().onEvent(TimelineEvent.UndoDelete)

        coVerify { trashFlow.undo() }
    }
}
