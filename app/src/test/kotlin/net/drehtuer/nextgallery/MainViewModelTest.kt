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
import net.drehtuer.nextgallery.core.data.edit.EditRepository
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.MediaAccess
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
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
    private val scheduler = mockk<IndexScheduler>(relaxed = true) {
        coEvery { observeMediaChanges() } coAnswers { awaitCancellation() }
    }

    private var settings = UserSettings()
    private val settingsRepository = mockk<SettingsRepository> {
        every { observeSettings() } answers { flowOf(settings) }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val editRepository = mockk<EditRepository>(relaxed = true)

    private fun viewModel() = MainViewModel(accessRepository, scheduler, editRepository, settingsRepository)

    @Test
    fun onEvent_firstResume_reportsAppStart() {
        viewModel().onEvent(MainEvent.Resumed)

        coVerify { scheduler.onAppStart(MediaAccess.FULL) }
    }

    @Test
    fun onEvent_firstResume_cleansUpAfterAnInterruptedSave() {
        viewModel().onEvent(MainEvent.Resumed)

        coVerify(exactly = 1) { editRepository.cleanupPending() }
    }

    @Test
    fun onEvent_laterResume_doesNotCleanUpAgain() {
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)

        viewModel.onEvent(MainEvent.Resumed)

        coVerify(exactly = 1) { editRepository.cleanupPending() }
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
    fun state_containsAccess() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)

        assertEquals(MainUiState(MediaAccess.FULL), viewModel.state.first { it.access != null })
    }

    @Test
    fun state_containsThemeAccentAndColumnsFromSettings() = runTest {
        settings = UserSettings(themeMode = ThemeMode.DARK, accentColor = AccentColor.CORAL, gridColumns = 5)
        val viewModel = viewModel()
        viewModel.onEvent(MainEvent.Resumed)

        assertEquals(
            MainUiState(MediaAccess.FULL, ThemeMode.DARK, AccentColor.CORAL, 5),
            viewModel.state.first { it.access != null },
        )
    }

    @Test
    fun requestedPermissions_comeFromAccessRepository() {
        assertEquals(listOf("permission"), viewModel().requestedPermissions)
    }
}
