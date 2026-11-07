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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.settings.FolderSettingsRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoldersSettingsViewModelTest {
    private val camera = AddedFolder("external_primary", "DCIM/Camera")
    private val settings = MutableStateFlow(UserSettings(addedFolders = listOf(camera)))
    private val settingsRepository = mockk<SettingsRepository> { every { observeSettings() } returns settings }
    private val folderSettings = mockk<FolderSettingsRepository>(relaxed = true)
    private val folderRepository = mockk<FolderRepository> {
        every { observeFolders() } returns flowOf(emptyList())
        every { observeVirtualFolders() } returns flowOf(emptyList())
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FoldersSettingsViewModel(folderRepository, settingsRepository, folderSettings)

    private suspend fun FoldersSettingsViewModel.content() =
        state.filterIsInstance<FoldersSettingsUiState.Content>().first()

    @Test
    fun state_beforeFoldersAreRead_isLoading() = runTest {
        every { folderRepository.observeFolders() } returns emptyFlow()

        assertEquals(FoldersSettingsUiState.Loading, viewModel().state.first())
    }

    @Test
    fun state_listsTheAddedFolders() = runTest {
        assertEquals(listOf(camera), viewModel().content().addedFolders)
    }

    @Test
    fun onEvent_folderPicked_addsIt() {
        viewModel().onEvent(FoldersSettingsEvent.FolderPicked("primary:DCIM"))

        coVerify { folderSettings.addFolder(AddedFolder("external_primary", "DCIM")) }
    }

    @Test
    fun onEvent_unusableFolderPicked_addsNothing() {
        viewModel().onEvent(FoldersSettingsEvent.FolderPicked("primary:"))

        coVerify(exactly = 0) { folderSettings.addFolder(any()) }
    }

    @Test
    fun onEvent_unusableFolderPicked_showsTheMessage() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(FoldersSettingsEvent.FolderPicked("primary:"))

        assertTrue(viewModel.content().addFailed)
    }

    @Test
    fun onEvent_usableFolderPickedAfterUnusableOne_hidesTheMessage() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.FolderPicked("primary:"))

        viewModel.onEvent(FoldersSettingsEvent.FolderPicked("primary:DCIM"))

        assertFalse(viewModel.content().addFailed)
    }

    @Test
    fun onEvent_removeFolder_removesIt() {
        viewModel().onEvent(FoldersSettingsEvent.RemoveFolder(camera))

        coVerify { folderSettings.removeFolder(camera) }
    }

    @Test
    fun onEvent_createVirtualFolder_opensAnEmptyEditor() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(FoldersSettingsEvent.CreateVirtualFolder)

        assertEquals(VirtualFolderEditor(null, "", emptySet()), viewModel.content().editor)
    }

    @Test
    fun onEvent_editVirtualFolder_opensTheEditorWithItsValues() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(FoldersSettingsEvent.EditVirtualFolder(VirtualFolder("v1", "All", listOf(7, 8))))

        assertEquals(VirtualFolderEditor("v1", "All", setOf(7, 8)), viewModel.content().editor)
    }

    @Test
    fun onEvent_deleteVirtualFolder_deletesIt() {
        viewModel().onEvent(FoldersSettingsEvent.DeleteVirtualFolder("v1"))

        coVerify { folderSettings.deleteVirtualFolder("v1") }
    }

    @Test
    fun onEvent_editorNameChanged_updatesTheName() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.CreateVirtualFolder)

        viewModel.onEvent(FoldersSettingsEvent.EditorNameChanged("All"))

        assertEquals("All", viewModel.content().editor?.name)
    }

    @Test
    fun onEvent_editorMemberToggled_addsAMember() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.CreateVirtualFolder)

        viewModel.onEvent(FoldersSettingsEvent.EditorMemberToggled(7))

        assertEquals(setOf(7L), viewModel.content().editor?.bucketIds)
    }

    @Test
    fun onEvent_editorMemberToggledAgain_removesTheMember() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.EditVirtualFolder(VirtualFolder("v1", "All", listOf(7, 8))))

        viewModel.onEvent(FoldersSettingsEvent.EditorMemberToggled(7))

        assertEquals(setOf(8L), viewModel.content().editor?.bucketIds)
    }

    @Test
    fun onEvent_editorSavedForNewFolder_createsItWithTheTrimmedName() {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.CreateVirtualFolder)
        viewModel.onEvent(FoldersSettingsEvent.EditorNameChanged(" All "))
        viewModel.onEvent(FoldersSettingsEvent.EditorMemberToggled(7))

        viewModel.onEvent(FoldersSettingsEvent.EditorSaved)

        coVerify { folderSettings.createVirtualFolder("All", listOf(7)) }
    }

    @Test
    fun onEvent_editorSavedForExistingFolder_updatesIt() {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.EditVirtualFolder(VirtualFolder("v1", "Old", listOf(7))))
        viewModel.onEvent(FoldersSettingsEvent.EditorNameChanged("New"))

        viewModel.onEvent(FoldersSettingsEvent.EditorSaved)

        coVerify { folderSettings.updateVirtualFolder(VirtualFolder("v1", "New", listOf(7))) }
    }

    @Test
    fun onEvent_editorSaved_closesTheEditor() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.EditVirtualFolder(VirtualFolder("v1", "All", listOf(7))))

        viewModel.onEvent(FoldersSettingsEvent.EditorSaved)

        assertNull(viewModel.content().editor)
    }

    @Test
    fun onEvent_editorSavedWithoutName_savesNothing() {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.CreateVirtualFolder)
        viewModel.onEvent(FoldersSettingsEvent.EditorMemberToggled(7))

        viewModel.onEvent(FoldersSettingsEvent.EditorSaved)

        coVerify(exactly = 0) { folderSettings.createVirtualFolder(any(), any()) }
    }

    @Test
    fun onEvent_editorClosed_savesNothingAndClosesIt() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FoldersSettingsEvent.EditVirtualFolder(VirtualFolder("v1", "All", listOf(7))))

        viewModel.onEvent(FoldersSettingsEvent.EditorClosed)

        assertNull(viewModel.content().editor)
    }

    @Test
    fun canSave_nameAndMember_isTrue() {
        assertTrue(VirtualFolderEditor(null, "All", setOf(7)).canSave)
    }

    @Test
    fun canSave_blankName_isFalse() {
        assertFalse(VirtualFolderEditor(null, "  ", setOf(7)).canSave)
    }

    @Test
    fun canSave_noMember_isFalse() {
        assertFalse(VirtualFolderEditor(null, "All", emptySet()).canSave)
    }
}
