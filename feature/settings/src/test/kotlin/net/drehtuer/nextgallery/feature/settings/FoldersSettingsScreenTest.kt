package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.VirtualFolder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class FoldersSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val camera = AddedFolder("external_primary", "DCIM/Camera")
    private val virtual = VirtualFolder("v1", "Everything", listOf(7))
    private val cameraFolder = Folder(
        bucketId = 7,
        name = "Camera",
        relativePath = "DCIM/Camera/",
        itemCount = 1,
        coverItem = MediaItem(
            id = MediaId(1),
            volumeName = "external_primary",
            contentUri = "content://media/1",
            bucketId = 7,
            bucketName = "Camera",
            relativePath = "DCIM/Camera/",
            displayName = "IMG_1.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1,
            width = 1,
            height = 1,
            orientation = 0,
            dateTakenMillis = 1,
            dateSource = DateSource.DATE_TAKEN,
            takenLocalDate = LocalDate.of(2024, 3, 15),
            dateModifiedSeconds = 1,
            generationModified = 1,
            location = null,
        ),
    )

    private fun content(
        addedFolders: List<AddedFolder> = emptyList(),
        virtualFolders: List<VirtualFolderSummary> = emptyList(),
        folders: List<Folder> = emptyList(),
        addFailed: Boolean = false,
        editor: VirtualFolderEditor? = null,
    ) = FoldersSettingsUiState.Content(addedFolders, virtualFolders, folders, addFailed, editor)

    private fun show(
        state: FoldersSettingsUiState,
        onEvent: (FoldersSettingsEvent) -> Unit = {},
        onAddFolder: () -> Unit = {},
        onBack: () -> Unit = {},
    ) = composeRule.setContent { FoldersSettingsScreen(state, onEvent, onAddFolder, onBack) }

    @Test
    fun loading_showsLoadingIndicator() {
        show(FoldersSettingsUiState.Loading)

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun backClicked_goesBack() {
        var clicks = 0
        show(FoldersSettingsUiState.Loading, onBack = { clicks++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun content_saysThatRemovingTouchesNoFile() {
        show(content())

        composeRule
            .onNodeWithText(
                "NextGallery only shows the folders you add here, with their subfolders. " +
                    "Removing a folder only hides it in NextGallery. No file is touched.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun noFolders_saysSo() {
        show(content())

        composeRule.onNodeWithText("No folders added.").assertIsDisplayed()
    }

    @Test
    fun folder_showsNameAndPath() {
        show(content(addedFolders = listOf(camera)))

        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("DCIM/Camera").assertIsDisplayed()
    }

    @Test
    fun folderOnSdCard_showsItsVolume() {
        show(content(addedFolders = listOf(AddedFolder("1234-abcd", "DCIM"))))

        composeRule.onNodeWithText("1234-abcd · DCIM").assertIsDisplayed()
    }

    @Test
    fun removeClicked_reportsTheFolder() {
        var event: FoldersSettingsEvent? = null
        show(content(addedFolders = listOf(camera)), onEvent = { event = it })

        composeRule.onNodeWithContentDescription("Remove Camera").performClick()

        assertEquals(FoldersSettingsEvent.RemoveFolder(camera), event)
    }

    @Test
    fun addFolderClicked_opensThePicker() {
        var clicks = 0
        show(content(), onAddFolder = { clicks++ })

        composeRule.onNodeWithText("Add folder").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun addFailed_showsTheMessage() {
        show(content(addFailed = true))

        composeRule
            .onNodeWithText(
                "This folder can't be added. Choose a folder on the phone's storage or on an SD card, " +
                    "not the top level of the storage.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun virtualFolder_showsNameAndCount() {
        show(content(virtualFolders = listOf(VirtualFolderSummary(virtual, 0, null))))

        composeRule.onNodeWithText("Everything").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0 images").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun virtualFolderClicked_opensItsEditor() {
        var event: FoldersSettingsEvent? = null
        show(content(virtualFolders = listOf(VirtualFolderSummary(virtual, 0, null))), onEvent = { event = it })

        composeRule.onNodeWithText("Everything").performScrollTo().performClick()

        assertEquals(FoldersSettingsEvent.EditVirtualFolder(virtual), event)
    }

    @Test
    fun deleteClicked_reportsTheVirtualFolder() {
        var event: FoldersSettingsEvent? = null
        show(content(virtualFolders = listOf(VirtualFolderSummary(virtual, 0, null))), onEvent = { event = it })

        composeRule.onNodeWithContentDescription("Delete Everything").performScrollTo().performClick()

        assertEquals(FoldersSettingsEvent.DeleteVirtualFolder("v1"), event)
    }

    @Test
    fun createVirtualFolder_withoutFolders_isDisabled() {
        show(content())

        composeRule.onNodeWithText("Create virtual folder").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun createVirtualFolderClicked_reportsIt() {
        var event: FoldersSettingsEvent? = null
        show(content(folders = listOf(cameraFolder)), onEvent = { event = it })

        composeRule.onNodeWithText("Create virtual folder").performScrollTo().performClick()

        assertEquals(FoldersSettingsEvent.CreateVirtualFolder, event)
    }

    @Test
    fun editor_newFolder_hasThatTitle() {
        show(content(editor = VirtualFolderEditor(null, "", emptySet())))

        composeRule.onNodeWithText("New virtual folder").assertIsDisplayed()
    }

    @Test
    fun editor_existingFolder_hasThatTitle() {
        show(content(editor = VirtualFolderEditor("v1", "All", setOf(7))))

        composeRule.onNodeWithText("Edit virtual folder").assertIsDisplayed()
    }

    @Test
    fun editor_backClicked_closesTheEditorOnly() {
        var event: FoldersSettingsEvent? = null
        var backs = 0
        show(
            content(editor = VirtualFolderEditor(null, "", emptySet())),
            onEvent = { event = it },
            onBack = { backs++ },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(FoldersSettingsEvent.EditorClosed to 0, event to backs)
    }

    @Test
    fun editor_nameTyped_isReported() {
        var event: FoldersSettingsEvent? = null
        show(content(editor = VirtualFolderEditor(null, "", emptySet())), onEvent = { event = it })

        composeRule.onNodeWithText("Name").performTextInput("All")

        assertEquals(FoldersSettingsEvent.EditorNameChanged("All"), event)
    }

    @Test
    fun editor_memberRow_showsWhetherItIsChosen() {
        show(content(folders = listOf(cameraFolder), editor = VirtualFolderEditor(null, "", setOf(7))))

        composeRule.onNodeWithText("Camera").assertIsOn()
    }

    @Test
    fun editor_otherFolderRow_isNotChosen() {
        show(content(folders = listOf(cameraFolder), editor = VirtualFolderEditor(null, "", setOf(8))))

        composeRule.onNodeWithText("Camera").assertIsOff()
    }

    @Test
    fun editor_memberClicked_reportsIt() {
        var event: FoldersSettingsEvent? = null
        show(
            content(folders = listOf(cameraFolder), editor = VirtualFolderEditor(null, "", emptySet())),
            onEvent = { event = it },
        )

        composeRule.onNodeWithText("Camera").performClick()

        assertEquals(FoldersSettingsEvent.EditorMemberToggled(7), event)
    }

    @Test
    fun editor_cannotSave_saveIsDisabled() {
        show(content(editor = VirtualFolderEditor(null, "", emptySet())))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun editor_canSave_saveIsEnabled() {
        show(content(editor = VirtualFolderEditor(null, "All", setOf(7))))

        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun editor_saveClicked_reportsIt() {
        var event: FoldersSettingsEvent? = null
        show(content(editor = VirtualFolderEditor(null, "All", setOf(7))), onEvent = { event = it })

        composeRule.onNodeWithText("Save").performClick()

        assertEquals(FoldersSettingsEvent.EditorSaved, event)
    }

    @Test
    fun actions_allHaveANameAndATouchTarget() {
        show(
            content(
                addedFolders = listOf(camera),
                virtualFolders = listOf(VirtualFolderSummary(virtual, 0, null)),
                folders = listOf(cameraFolder),
            ),
        )

        composeRule.onAllNodes(hasClickAction()).assertAll(namedTouchTarget)
    }

    @Test
    fun editorActions_allHaveANameAndATouchTarget() {
        show(content(folders = listOf(cameraFolder), editor = VirtualFolderEditor(null, "All", setOf(7))))

        composeRule.onAllNodes(hasClickAction()).assertAll(namedTouchTarget)
    }
}

/** A screen reader can say what it does, and a finger can hit it. */
private val namedTouchTarget = SemanticsMatcher("has a name and a 48 dp touch target") { node ->
    val named = SemanticsProperties.Text in node.config || SemanticsProperties.ContentDescription in node.config
    val minimum = with(node.layoutInfo.density) { 48.dp.roundToPx() }
    val touch = node.touchBoundsInRoot
    named && maxOf(touch.width.toInt(), node.size.width) >= minimum &&
        maxOf(touch.height.toInt(), node.size.height) >= minimum
}
