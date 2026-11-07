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
import net.drehtuer.nextgallery.core.model.FolderRule
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RuleChangeObserverTest {
    private val settings = MutableStateFlow(UserSettings())
    private val repository = mockk<SettingsRepository> { every { observeSettings() } returns settings }
    private val scheduler = FakeWorkScheduler()

    private fun TestScope.start() {
        backgroundScope.launch { RuleChangeObserver(repository, scheduler).observe() }
        runCurrent()
    }

    private fun TestScope.waitForDebounce() {
        advanceTimeBy(RuleChangeObserver.DEBOUNCE_MILLIS + 1)
        runCurrent()
    }

    @Test
    fun observe_noChange_enqueuesNothing() = runTest {
        start()

        waitForDebounce()

        assertEquals(emptyList<IndexMode>(), scheduler.enqueued)
    }

    @Test
    fun observe_ruleChange_enqueuesOneFullScan() = runTest {
        start()

        settings.value = UserSettings(folderRules = listOf(FolderRule("DCIM", FolderRule.Type.EXCLUDE)))
        waitForDebounce()

        assertEquals(listOf(IndexMode.FULL), scheduler.enqueued)
    }

    @Test
    fun observe_patternChange_enqueuesOneFullScan() = runTest {
        start()

        settings.value = UserSettings(excludePatterns = listOf("Old*"))
        waitForDebounce()

        assertEquals(listOf(IndexMode.FULL), scheduler.enqueued)
    }

    @Test
    fun observe_quickChanges_enqueueOneFullScan() = runTest {
        start()

        settings.value = UserSettings(excludePatterns = listOf("a"))
        runCurrent()
        settings.value = UserSettings(excludePatterns = listOf("b"))
        waitForDebounce()

        assertEquals(listOf(IndexMode.FULL), scheduler.enqueued)
    }

    @Test
    fun observe_otherSettingChange_enqueuesNothing() = runTest {
        start()

        settings.value = UserSettings(themeMode = ThemeMode.DARK)
        waitForDebounce()

        assertEquals(emptyList<IndexMode>(), scheduler.enqueued)
    }
}
