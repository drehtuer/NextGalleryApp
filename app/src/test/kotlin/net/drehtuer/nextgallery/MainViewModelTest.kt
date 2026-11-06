package net.drehtuer.nextgallery

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.access.MediaAccessRepository
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.model.MediaAccess
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private var access = MediaAccess.FULL
    private val accessRepository = mockk<MediaAccessRepository> {
        every { currentAccess() } answers { access }
        every { requestedPermissions } returns listOf("permission")
    }
    private val mediaRepository = mockk<MediaRepository> { every { observeIndexedCount() } returns flowOf(3) }
    private val scheduler = mockk<IndexScheduler>(relaxed = true) {
        coEvery { observeMediaChanges() } coAnswers { awaitCancellation() }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MainViewModel(accessRepository, mediaRepository, scheduler)

    @Test
    fun onEvent_firstResume_reportsAppStart() {
        viewModel().onEvent(MainEvent.Resumed)

        coVerify { scheduler.onAppStart(MediaAccess.FULL) }
    }

    @Test
    fun onEvent_resumeWithUnchangedAccess_doesNotReportAccessChange() {
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)

        viewModel.onEvent(MainEvent.Resumed)

        verify(exactly = 0) { scheduler.onAccessChanged(any()) }
    }

    @Test
    fun onEvent_resumeWithChangedAccess_reportsAccessChange() {
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)
        access = MediaAccess.PARTIAL

        viewModel.onEvent(MainEvent.Resumed)

        verify { scheduler.onAccessChanged(MediaAccess.PARTIAL) }
    }

    @Test
    fun onEvent_accessRequestFinishedWithSameAccess_reportsAccessChange() {
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)

        viewModel.onEvent(MainEvent.AccessRequestFinished)

        verify { scheduler.onAccessChanged(MediaAccess.FULL) }
    }

    @Test
    fun onEvent_accessGranted_startsObservingMediaChangesOnce() {
        val viewModel = viewModel()

        viewModel.onEvent(MainEvent.Resumed)
        viewModel.onEvent(MainEvent.Resumed)

        coVerify(exactly = 1) { scheduler.observeMediaChanges() }
    }

    @Test
    fun onEvent_accessDenied_doesNotObserveMediaChanges() {
        access = MediaAccess.DENIED

        viewModel().onEvent(MainEvent.Resumed)

        coVerify(exactly = 0) { scheduler.observeMediaChanges() }
    }

    @Test
    fun state_combinesAccessAndIndexedCount() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)

        assertEquals(MainUiState(MediaAccess.FULL, 3), viewModel.state.first { it.access != null })
    }

    @Test
    fun requestedPermissions_comeFromAccessRepository() {
        assertEquals(listOf("permission"), viewModel().requestedPermissions)
    }
}
