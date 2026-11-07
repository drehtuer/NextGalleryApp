package net.drehtuer.nextgallery.feature.remote

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.CertificateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteBrowserScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RemoteBrowserEvent>()
    private var signIn: Boolean? = null
    private val loaded = RemoteBrowserUiState(
        folders = listOf(FOLDER),
        images = listOf(IMAGE_A, IMAGE_B),
        urls = FakeUrls,
        isLoading = false,
    )

    private fun show(state: RemoteBrowserUiState) = composeRule.setContent {
        RemoteBrowserScreen(state, { events += it }, onSignIn = { signIn = it })
    }

    @Test
    fun loginRequired_showsTheHintWithoutBrowser() {
        show(RemoteBrowserUiState(loginRequired = true))

        composeRule.onNodeWithText("Connect your Nextcloud").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Refresh").assertDoesNotExist()
    }

    @Test
    fun loginRequired_signInOpensTheLogin() {
        show(RemoteBrowserUiState(loginRequired = true))

        composeRule.onNodeWithText("Sign in").performClick()

        assertEquals(false, signIn)
    }

    @Test
    fun loaded_showsFoldersWithCountsAndImages() {
        show(loaded)

        composeRule.onNodeWithText("Italy").assertIsDisplayed()
        composeRule.onNodeWithText("3 items").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("a.jpg", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun folderClicked_opensIt() {
        show(loaded)

        composeRule.onNodeWithText("Italy").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(BrowseEvent.OpenFolder(FOLDER.path)), events)
    }

    @Test
    fun breadcrumbClicked_opensThatFolder() {
        show(loaded.copy(path = "/P/Italy"))

        composeRule.onNodeWithText("P").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(BrowseEvent.OpenFolder("/P")), events)
    }

    @Test
    fun refreshClicked_refreshes() {
        show(loaded)

        composeRule.onNodeWithContentDescription("Refresh").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(BrowseEvent.Refresh), events)
    }

    @Test
    fun selectClicked_startsSelecting() {
        show(loaded)

        composeRule.onNodeWithContentDescription("Select").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.StartSelecting), events)
    }

    @Test
    fun backInSubfolder_goesUp() {
        show(loaded.copy(path = "/P/Italy"))

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(BrowseEvent.Up), events)
    }

    @Test
    fun loading_showsPlaceholders() {
        show(RemoteBrowserUiState(isLoading = true))

        composeRule.onNodeWithText("Nothing here").assertDoesNotExist()
    }

    @Test
    fun empty_explains() {
        show(RemoteBrowserUiState(isLoading = false))

        composeRule.onNodeWithText("Nothing here").assertIsDisplayed()
    }

    @Test
    fun error_showsItWithRetry() {
        show(RemoteBrowserUiState(isLoading = false, error = AppError.Server(500)))

        composeRule.onNodeWithText("Couldn't load this folder").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(BrowseEvent.Refresh), events)
    }

    @Test
    fun offline_showsTheBanner() {
        show(loaded.copy(serverUnreachable = true))

        composeRule.onNodeWithText("You're offline. Showing what was loaded before.").assertIsDisplayed()
    }

    @Test
    fun sessionEnded_showsTheBannerAndSignsInAgain() {
        show(loaded.copy(sessionEnded = true))

        composeRule.onNodeWithText("Your Nextcloud session ended. Sign in again to see new photos.").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in").performClick()

        assertEquals(true, signIn)
    }

    @Test
    fun longPress_sendsTheEvent() {
        show(loaded)

        composeRule.onNodeWithContentDescription("a.jpg", substring = true).performTouchInput { longClick() }

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.ItemLongPressed(IMAGE_A)), events)
    }

    @Test
    fun imageClicked_sendsTheEvent() {
        show(loaded)

        composeRule.onNodeWithContentDescription("a.jpg", substring = true).performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.ItemClicked(IMAGE_A)), events)
    }

    private val withMenu = loaded.copy(menu = RemoteMenuState(IMAGE_A.path, listOf(IMAGE_A)))

    @Test
    fun menu_showsTheFileNameAndActions() {
        show(withMenu)

        composeRule.onNodeWithText("a.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("Move to folder…").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun menu_forSeveralImages_showsTheirNumber() {
        show(loaded.copy(menu = RemoteMenuState(IMAGE_A.path, listOf(IMAGE_A, IMAGE_B))))

        composeRule.onNodeWithText("2 selected images").assertIsDisplayed()
    }

    @Test
    fun menu_moveSendsTheEvent() {
        show(withMenu)

        composeRule.onNodeWithText("Move to folder…").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.MenuMove), events)
    }

    @Test
    fun menu_shareSendsTheEvent() {
        show(withMenu)

        composeRule.onNodeWithText("Share").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.MenuShare), events)
    }

    @Test
    fun menu_deleteSendsTheEvent() {
        show(withMenu)

        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.MenuDelete), events)
    }

    private val selecting = loaded.copy(selecting = true, selection = mapOf(IMAGE_A.path to IMAGE_A))

    @Test
    fun selecting_showsTheCountAndMarksTheImage() {
        show(selecting)

        composeRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("a.jpg", substring = true).assertIsSelected()
    }

    @Test
    fun selecting_selectAllSendsTheEvent() {
        show(selecting)

        composeRule.onNodeWithContentDescription("Select all").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.SelectAll), events)
    }

    @Test
    fun selecting_moveSendsTheEvent() {
        show(selecting)

        composeRule.onNodeWithContentDescription("Move").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.MoveSelected), events)
    }

    @Test
    fun selecting_nothingSelected_hasNoMove() {
        show(loaded.copy(selecting = true))

        composeRule.onNodeWithContentDescription("Move").assertDoesNotExist()
    }

    @Test
    fun selecting_clearSendsTheEvent() {
        show(selecting)

        composeRule.onNodeWithContentDescription("Clear selection").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(SelectionEvent.ClearSelection), events)
    }

    @Test
    fun certificate_showsTheDialogAndSendsTheDecision() {
        val info = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")
        show(loaded.copy(certificate = info))

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(BrowseEvent.RejectCertificate), events)
    }

    @Test
    fun share_showsProgressAndCancels() {
        show(loaded.copy(share = RemoteShareUiState(2, 3)))

        composeRule.onNodeWithText("Downloading 2 of 3…").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(ActionEvent.CancelShare), events)
    }

    @Test
    fun moveSheetContent_noFolderButTitleAndIntro() {
        composeRule.setContent {
            MoveSheetContent(MoveSheetState(listOf(IMAGE_A, IMAGE_B), "/", isLoading = false), {})
        }

        composeRule.onNodeWithText("Move 2 images").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a folder on your Nextcloud.", substring = true).assertIsDisplayed()
    }

    @Test
    fun moveSheetContent_folderClickOpensIt() {
        val sheet = MoveSheetState(listOf(IMAGE_A), "/", listOf(MoveFolder(FOLDER, canWrite = true)), isLoading = false)
        composeRule.setContent { MoveSheetContent(sheet, { events += it }) }

        composeRule.onNodeWithText("Italy").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.SheetFolderOpened(FOLDER.path)), events)
    }

    @Test
    fun moveSheetContent_readOnlyFolder_isDisabledWithExplanation() {
        val sheet =
            MoveSheetState(listOf(IMAGE_A), "/", listOf(MoveFolder(FOLDER, canWrite = false)), isLoading = false)
        composeRule.setContent { MoveSheetContent(sheet, {}) }

        composeRule.onNodeWithText("Read-only share: you can't add files here").assertIsDisplayed()
        composeRule.onNodeWithText("Italy").assertIsNotEnabled()
    }

    @Test
    fun moveSheetContent_showsTheTarget() {
        val sheet = MoveSheetState(listOf(IMAGE_A), "/Photos/2024/Holiday/Italy", isLoading = false)
        composeRule.setContent { MoveSheetContent(sheet, {}) }

        composeRule.onNodeWithText("To: Photos/2024/Holiday/Italy").assertIsDisplayed()
    }

    @Test
    fun moveSheetContent_ownFolder_moveHereIsDisabledWithExplanation() {
        val sheet = MoveSheetState(listOf(IMAGE_A), IMAGE_A.parentPath, isLoading = false)
        composeRule.setContent { MoveSheetContent(sheet, {}) }

        composeRule.onNodeWithText("The images are already in this folder.").assertIsDisplayed()
        composeRule.onNodeWithText("Move here").assertIsNotEnabled()
    }

    @Test
    fun moveSheetContent_readOnlyTarget_moveHereIsDisabled() {
        val sheet = MoveSheetState(listOf(IMAGE_A), "/ro", isLoading = false, canWriteHere = false)
        composeRule.setContent { MoveSheetContent(sheet, {}) }

        composeRule.onNodeWithText("Move here").assertIsNotEnabled()
        composeRule.onNodeWithText("New folder").assertIsNotEnabled()
    }

    @Test
    fun moveSheetContent_moveHereSendsTheEvent() {
        val sheet = MoveSheetState(listOf(IMAGE_A), "/Italy", isLoading = false)
        composeRule.setContent { MoveSheetContent(sheet, { events += it }) }

        composeRule.onNodeWithText("Move here").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.MoveHere), events)
    }

    @Test
    fun moveSheetContent_error_isShown() {
        val sheet = MoveSheetState(listOf(IMAGE_A), "/x", isLoading = false, error = AppError.NotFound)
        composeRule.setContent { MoveSheetContent(sheet, {}) }

        composeRule.onNodeWithText("This is no longer on your server.").assertIsDisplayed()
    }

    @Test
    fun moveSheetContent_newFolderSendsTheEvent() {
        val sheet = MoveSheetState(listOf(IMAGE_A), "/Italy", isLoading = false)
        composeRule.setContent { MoveSheetContent(sheet, { events += it }) }

        composeRule.onNodeWithText("New folder").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.NewFolderRequested), events)
    }

    @Test
    fun actions_allHaveANameAndATouchTarget() {
        show(loaded.copy(path = "/Photos/2024"))

        val actions = composeRule.onAllNodes(hasClickAction())

        assertTrue(actions.fetchSemanticsNodes().isNotEmpty())
        actions.assertAll(namedTouchTarget)
    }

    @Test
    fun selectedImage_oneNodeHasItsNameItsStateAndItsAction() {
        show(loaded.copy(selection = mapOf(IMAGE_A.path to IMAGE_A)))

        composeRule.onNode(hasContentDescription(IMAGE_A.name, substring = true))
            .assertIsSelected()
            .assert(hasClickAction())
    }

    @Test
    fun image_longPressIsAnnouncedAsShowingActions() {
        show(loaded)

        val image = composeRule.onNode(hasContentDescription(IMAGE_A.name, substring = true)).fetchSemanticsNode()

        assertEquals("Show actions", image.config[SemanticsActions.OnLongClick].label)
    }
}

/** A screen reader can say what it does, and a finger can hit it. */
private val namedTouchTarget = SemanticsMatcher("has a name and a 48 dp touch target") { node ->
    val named = SemanticsProperties.Text in node.config || SemanticsProperties.ContentDescription in node.config
    val minimum = with(node.layoutInfo.density) { 48.dp.roundToPx() }
    // The touch bounds are empty for what is scrolled out of the window, so these count the layout size as well.
    val touch = node.touchBoundsInRoot
    named && maxOf(touch.width.toInt(), node.size.width) >= minimum &&
        maxOf(touch.height.toInt(), node.size.height) >= minimum
}
