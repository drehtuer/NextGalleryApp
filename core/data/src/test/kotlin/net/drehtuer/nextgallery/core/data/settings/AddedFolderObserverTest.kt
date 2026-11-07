package net.drehtuer.nextgallery.core.data.settings

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeWorkScheduler
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolder
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddedFolderObserverTest {
    private val settings = MutableStateFlow(UserSettings())
    private val repository = mockk<SettingsRepository> { every { observeSettings() } returns settings }
    private val scheduler = FakeWorkScheduler()
    private val camera = AddedFolder("external_primary", "DCIM/Camera")

    private fun TestScope.start() {
        backgroundScope.launch { AddedFolderObserver(repository, scheduler).observe() }
        runCurrent()
    }

    private fun TestScope.waitForDebounce() {
        advanceTimeBy(AddedFolderObserver.DEBOUNCE_MILLIS + 1)
        runCurrent()
    }

    @Test
    fun observe_noChange_enqueuesNothing() = runTest {
        start()

        waitForDebounce()

        assertEquals(emptyList<IndexMode>(), scheduler.enqueued)
    }

    @Test
    fun observe_folderAdded_enqueuesOneFullScan() = runTest {
        start()

        settings.value = UserSettings(addedFolders = listOf(camera))
        waitForDebounce()

        assertEquals(listOf(IndexMode.FULL), scheduler.enqueued)
    }

    @Test
    fun observe_folderRemoved_enqueuesOneFullScan() = runTest {
        settings.value = UserSettings(addedFolders = listOf(camera))
        start()

        settings.value = UserSettings()
        waitForDebounce()

        assertEquals(listOf(IndexMode.FULL), scheduler.enqueued)
    }

    @Test
    fun observe_quickChanges_enqueueOneFullScan() = runTest {
        start()

        settings.value = UserSettings(addedFolders = listOf(camera))
        runCurrent()
        settings.value = UserSettings()
        settings.value = UserSettings(addedFolders = listOf(AddedFolder("external_primary", "Pictures")))
        waitForDebounce()

        assertEquals(listOf(IndexMode.FULL), scheduler.enqueued)
    }

    @Test
    fun observe_virtualFolderChange_enqueuesNothing() = runTest {
        start()

        settings.value = UserSettings(virtualFolders = listOf(VirtualFolder("v1", "All", listOf(7))))
        waitForDebounce()

        assertEquals(emptyList<IndexMode>(), scheduler.enqueued)
    }

    @Test
    fun observe_otherSettingChange_enqueuesNothing() = runTest {
        start()

        settings.value = UserSettings(themeMode = ThemeMode.DARK)
        waitForDebounce()

        assertEquals(emptyList<IndexMode>(), scheduler.enqueued)
    }
}
