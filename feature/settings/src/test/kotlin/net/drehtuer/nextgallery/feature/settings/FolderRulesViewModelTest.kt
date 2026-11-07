package net.drehtuer.nextgallery.feature.settings

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.DeviceFolder
import net.drehtuer.nextgallery.core.model.FolderRule
import net.drehtuer.nextgallery.core.model.FolderRule.Type.EXCLUDE
import net.drehtuer.nextgallery.core.model.FolderRule.Type.INCLUDE
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRulesViewModelTest {
    private val camera = DeviceFolder("Camera", "DCIM/Camera", 2)
    private val settings = MutableStateFlow(UserSettings(folderRules = emptyList()))
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns settings
    }
    private val folderRepository = mockk<FolderRepository> {
        every { observeAllDeviceFolders() } returns flowOf(listOf(camera))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FolderRulesViewModel(folderRepository, settingsRepository)

    private suspend fun FolderRulesViewModel.content() = state.filterIsInstance<FolderRulesUiState.Content>().first()

    @Test
    fun state_beforeFoldersAreRead_isLoading() = runTest {
        every { folderRepository.observeAllDeviceFolders() } returns emptyFlow()

        assertEquals(FolderRulesUiState.Loading, viewModel().state.first())
    }

    @Test
    fun state_folderWithoutRule_isDefault() = runTest {
        assertEquals(RuleChoice.DEFAULT, viewModel().content().rows.single().choice)
    }

    @Test
    fun state_readingFoldersFails_stillShowsRules() = runTest {
        every { folderRepository.observeAllDeviceFolders() } returns flow { error("no access") }
        settings.value = UserSettings(folderRules = listOf(FolderRule("DCIM", EXCLUDE)))

        assertEquals("DCIM", viewModel().content().rows.single().relativePath)
    }

    @Test
    fun state_storedPatterns_areOnePerLine() = runTest {
        settings.value = UserSettings(excludePatterns = listOf("a*", "b"))

        assertEquals("a*\nb", viewModel().content().patterns)
    }

    @Test
    fun onEvent_include_addsIncludeRule() {
        viewModel().onEvent(FolderRulesEvent.ChoiceChanged("DCIM/Camera", RuleChoice.INCLUDE))

        coVerify { settingsRepository.addRule(FolderRule("DCIM/Camera", INCLUDE)) }
    }

    @Test
    fun onEvent_exclude_addsExcludeRule() {
        viewModel().onEvent(FolderRulesEvent.ChoiceChanged("DCIM/Camera", RuleChoice.EXCLUDE))

        coVerify { settingsRepository.addRule(FolderRule("DCIM/Camera", EXCLUDE)) }
    }

    @Test
    fun onEvent_default_removesRule() {
        viewModel().onEvent(FolderRulesEvent.ChoiceChanged("DCIM/Camera", RuleChoice.DEFAULT))

        coVerify { settingsRepository.removeRule("DCIM/Camera") }
    }

    @Test
    fun onEvent_patternsChanged_storesParsedPatterns() {
        viewModel().onEvent(FolderRulesEvent.PatternsChanged("a*\n\n b "))

        coVerify { settingsRepository.setExcludePatterns(listOf("a*", "b")) }
    }

    @Test
    fun onEvent_patternsChanged_keepsTheTextAsTyped() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(FolderRulesEvent.PatternsChanged("a*\n"))

        assertEquals("a*\n", viewModel.content().patterns)
    }

    @Test
    fun folderRuleRows_includeRuleOnFolder_isInclude() {
        val rows = folderRuleRows(listOf(camera), listOf(FolderRule("dcim/camera", INCLUDE)))

        assertEquals(RuleChoice.INCLUDE, rows.single().choice)
    }

    @Test
    fun folderRuleRows_excludeRuleOnFolder_isExclude() {
        val rows = folderRuleRows(listOf(camera), listOf(FolderRule("DCIM/Camera", EXCLUDE)))

        assertEquals(RuleChoice.EXCLUDE, rows.single().choice)
    }

    @Test
    fun folderRuleRows_ruleWithoutFolder_getsOwnRowNamedAfterItsPath() {
        val rows = folderRuleRows(emptyList(), listOf(FolderRule("Android/data", EXCLUDE)))

        assertEquals(listOf(FolderRuleRow("data", "Android/data", 0, RuleChoice.EXCLUDE)), rows)
    }

    @Test
    fun folderRuleRows_areSortedByPath() {
        val rows = folderRuleRows(listOf(camera), listOf(FolderRule("Android/data", EXCLUDE)))

        assertEquals(listOf("Android/data", "DCIM/Camera"), rows.map { it.relativePath })
    }
}
