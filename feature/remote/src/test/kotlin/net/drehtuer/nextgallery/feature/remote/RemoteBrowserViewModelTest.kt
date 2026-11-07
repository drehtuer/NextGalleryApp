package net.drehtuer.nextgallery.feature.remote

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.nextcloud.ConflictPolicy
import net.drehtuer.nextgallery.core.data.nextcloud.ConnectivitySource
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBatchResult
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBrowseRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFolderContent
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteListing
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteMoveFlow
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteMoveResult
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareProgress
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareRepository
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.ShareSpec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteBrowserViewModelTest {
    private val urls = MutableStateFlow<RemoteImageUrls?>(FakeUrls)
    private val online = MutableStateFlow(true)
    private val accounts = mockk<NextcloudAccountRepository> { every { observeImageUrls() } returns urls }
    private val browse = mockk<RemoteBrowseRepository> {
        every { list(any()) } answers { flowOf(RemoteListing(firstArg(), emptyList())) }
        every { list("/") } returns flowOf(RemoteListing("/", listOf(FOLDER, IMAGE_A, IMAGE_B)))
    }
    private val operations = mockk<RemoteFileOperations> {
        coEvery { listFolder(any()) } returns AppResult.Success(RemoteFolderContent(null, emptyList()))
        every { canWrite(any()) } returns true
        coEvery { isTrashBinEnabled() } returns true
    }
    private val shares = mockk<RemoteShareRepository>(relaxed = true)
    private val certificates = mockk<TrustedCertificateRepository> {
        coEvery { accept(any()) } returns AppResult.Success(Unit)
    }
    private val connectivity = object : ConnectivitySource {
        override val isOnline = online
    }
    private val certificate = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = RemoteBrowserViewModel(
        RemoteBrowserDependencies(
            accounts,
            browse,
            operations,
            RemoteMoveFlow(operations),
            shares,
            certificates,
            connectivity,
        ),
    )

    private fun listing(vararg items: RemoteItem, error: AppError? = null) {
        every { browse.list("/") } returns flowOf(RemoteListing("/", items.toList(), error = error))
    }

    // Browsing

    @Test
    fun loggedOut_requiresLoginWithoutListing() {
        urls.value = null

        assertTrue(viewModel().state.value.loginRequired)
        verify(exactly = 0) { browse.list(any()) }
    }

    @Test
    fun loggedIn_listsTheRootAndSplitsFoldersAndImages() {
        val state = viewModel().state.value

        assertEquals(listOf(FOLDER), state.folders)
        assertEquals(listOf(IMAGE_A, IMAGE_B), state.images)
        assertFalse(state.isLoading)
    }

    @Test
    fun logout_clearsWhatWasShown() {
        val viewModel = viewModel()

        urls.value = null

        assertTrue(viewModel.state.value.loginRequired)
        assertTrue(viewModel.state.value.images.isEmpty())
    }

    @Test
    fun login_listsAgain() {
        urls.value = null
        val viewModel = viewModel()

        urls.value = FakeUrls

        assertFalse(viewModel.state.value.loginRequired)
        assertEquals(listOf(IMAGE_A, IMAGE_B), viewModel.state.value.images)
    }

    @Test
    fun openFolder_listsThatFolder() {
        val viewModel = viewModel()

        viewModel.onEvent(BrowseEvent.OpenFolder("/P/Italy"))

        assertEquals("/P/Italy", viewModel.state.value.path)
        verify { browse.list("/P/Italy") }
    }

    @Test
    fun up_listsTheParent() {
        val viewModel = viewModel()
        viewModel.onEvent(BrowseEvent.OpenFolder("/P/Italy"))

        viewModel.onEvent(BrowseEvent.Up)

        assertEquals("/P", viewModel.state.value.path)
    }

    @Test
    fun refresh_listsAgain() {
        val viewModel = viewModel()

        viewModel.onEvent(BrowseEvent.Refresh)

        verify(exactly = 2) { browse.list("/") }
    }

    @Test
    fun listing_whileRefreshingWithoutItems_isLoading() {
        every { browse.list("/") } returns flow {
            emit(RemoteListing("/", emptyList(), isRefreshing = true))
            kotlinx.coroutines.awaitCancellation()
        }

        assertTrue(viewModel().state.value.isLoading)
    }

    @Test
    fun listing_whileRefreshingWithCachedItems_showsThem() {
        every { browse.list("/") } returns flow {
            emit(RemoteListing("/", listOf(IMAGE_A), isRefreshing = true))
            kotlinx.coroutines.awaitCancellation()
        }

        val state = viewModel().state.value

        assertTrue(state.isRefreshing)
        assertFalse(state.isLoading)
        assertEquals(listOf(IMAGE_A), state.images)
    }

    @Test
    fun listing_networkError_isOfflineWithCachedItems() {
        listing(IMAGE_A, error = AppError.Network("gone"))

        val state = viewModel().state.value

        assertTrue(state.isOffline)
        assertNull(state.error)
        assertEquals(listOf(IMAGE_A), state.images)
    }

    @Test
    fun listing_auth_showsSessionEnded() {
        listing(error = AppError.Auth)

        assertTrue(viewModel().state.value.sessionEnded)
    }

    @Test
    fun listing_notLoggedIn_requiresLogin() {
        listing(error = AppError.NotLoggedIn)

        assertTrue(viewModel().state.value.loginRequired)
    }

    @Test
    fun listing_otherError_isShown() {
        listing(error = AppError.Server(500))

        assertEquals(AppError.Server(500), viewModel().state.value.error)
    }

    @Test
    fun listing_otherErrorWithCachedItems_reportsIt() = runTest {
        listing(IMAGE_A, error = AppError.Server(500))
        val viewModel = viewModel()

        viewModel.effects.test {
            assertEquals(RemoteBrowserEffect.Failed(AppError.Server(500)), awaitItem())
        }
    }

    @Test
    fun connectivityLost_isOffline() {
        val viewModel = viewModel()

        online.value = false

        assertTrue(viewModel.state.value.isOffline)
    }

    // Certificates

    @Test
    fun listing_untrustedCertificate_showsTheDialogAndPinsNothing() {
        listing(error = AppError.UntrustedCertificate(certificate))

        assertEquals(certificate, viewModel().state.value.certificate)
        coVerify(exactly = 0) { certificates.accept(any()) }
    }

    @Test
    fun trustCertificate_pinsAndRetries() {
        listing(error = AppError.UntrustedCertificate(certificate))
        val viewModel = viewModel()
        listing(IMAGE_A)

        viewModel.onEvent(BrowseEvent.TrustCertificate)

        coVerify { certificates.accept(certificate) }
        assertEquals(listOf(IMAGE_A), viewModel.state.value.images)
        assertNull(viewModel.state.value.certificate)
    }

    @Test
    fun trustCertificate_pinFails_showsTheError() {
        coEvery { certificates.accept(any()) } returns AppResult.Failure(AppError.Io("disk"))
        listing(error = AppError.UntrustedCertificate(certificate))
        val viewModel = viewModel()

        viewModel.onEvent(BrowseEvent.TrustCertificate)

        assertEquals(AppError.Io("disk"), viewModel.state.value.error)
    }

    @Test
    fun rejectCertificate_closesTheDialogAndKeepsTheErrorState() {
        listing(error = AppError.UntrustedCertificate(certificate))
        val viewModel = viewModel()

        viewModel.onEvent(BrowseEvent.RejectCertificate)

        assertNull(viewModel.state.value.certificate)
        assertEquals(AppError.UntrustedCertificate(certificate), viewModel.state.value.error)
        coVerify(exactly = 0) { certificates.accept(any()) }
    }

    @Test
    fun trustCertificate_withoutACertificate_doesNothing() {
        viewModel().onEvent(BrowseEvent.TrustCertificate)

        coVerify(exactly = 0) { certificates.accept(any()) }
    }

    // Selection and the context menu

    @Test
    fun itemClicked_notSelecting_opensTheViewer() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SelectionEvent.ItemClicked(IMAGE_A))

            assertEquals(RemoteBrowserEffect.OpenViewer(IMAGE_A.path), awaitItem())
        }
    }

    @Test
    fun itemClicked_selecting_togglesTheImage() {
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.StartSelecting)

        viewModel.onEvent(SelectionEvent.ItemClicked(IMAGE_A))
        assertEquals(setOf(IMAGE_A.path), viewModel.state.value.selection.keys)

        viewModel.onEvent(SelectionEvent.ItemClicked(IMAGE_A))
        assertTrue(viewModel.state.value.selection.isEmpty())
    }

    @Test
    fun longPress_notSelecting_opensTheMenuForThatImageOnly() {
        val viewModel = viewModel()

        viewModel.onEvent(SelectionEvent.ItemLongPressed(IMAGE_A))

        assertEquals(RemoteMenuState(IMAGE_A.path, listOf(IMAGE_A)), viewModel.state.value.menu)
        assertTrue(viewModel.state.value.selection.isEmpty())
    }

    @Test
    fun longPress_selecting_addsTheImageAndTheMenuActsOnAll() {
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.StartSelecting)
        viewModel.onEvent(SelectionEvent.ItemClicked(IMAGE_A))

        viewModel.onEvent(SelectionEvent.ItemLongPressed(IMAGE_B))

        assertEquals(setOf(IMAGE_A.path, IMAGE_B.path), viewModel.state.value.selection.keys)
        assertEquals(RemoteMenuState(IMAGE_B.path, listOf(IMAGE_A, IMAGE_B)), viewModel.state.value.menu)
    }

    @Test
    fun selectAll_selectsTheImagesOfTheFolderButNotItsFolders() {
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.StartSelecting)

        viewModel.onEvent(SelectionEvent.SelectAll)

        assertEquals(setOf(IMAGE_A.path, IMAGE_B.path), viewModel.state.value.selection.keys)
    }

    @Test
    fun clearSelection_endsSelecting() {
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.StartSelecting)
        viewModel.onEvent(SelectionEvent.SelectAll)

        viewModel.onEvent(SelectionEvent.ClearSelection)

        assertFalse(viewModel.state.value.selecting)
        assertTrue(viewModel.state.value.selection.isEmpty())
    }

    @Test
    fun menuDismissed_closesTheMenu() {
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.ItemLongPressed(IMAGE_A))

        viewModel.onEvent(SelectionEvent.MenuDismissed)

        assertNull(viewModel.state.value.menu)
    }

    @Test
    fun menuAction_withoutMenu_doesNothing() {
        val viewModel = viewModel()

        viewModel.onEvent(SelectionEvent.MenuMove)

        assertNull(viewModel.state.value.move.sheet)
    }

    // Moving

    private fun RemoteBrowserViewModel.openMoveSheet() {
        onEvent(SelectionEvent.ItemLongPressed(IMAGE_A))
        onEvent(SelectionEvent.MenuMove)
    }

    @Test
    fun menuMove_opensTheSheetInTheCurrentFolder() {
        val viewModel = viewModel()

        viewModel.openMoveSheet()

        assertEquals(listOf(IMAGE_A), viewModel.state.value.move.sheet?.items)
        assertEquals("/", viewModel.state.value.move.sheet?.path)
        assertNull(viewModel.state.value.menu)
    }

    @Test
    fun menuMove_offline_explainsAndOpensNothing() = runTest {
        online.value = false
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.openMoveSheet()

            assertEquals(RemoteBrowserEffect.Offline(RemoteOfflineAction.MOVE), awaitItem())
        }
        assertNull(viewModel.state.value.move.sheet)
    }

    @Test
    fun moveSelected_opensTheSheetForTheSelection() {
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.StartSelecting)
        viewModel.onEvent(SelectionEvent.SelectAll)

        viewModel.onEvent(SelectionEvent.MoveSelected)

        assertEquals(listOf(IMAGE_A, IMAGE_B), viewModel.state.value.move.sheet?.items)
    }

    @Test
    fun moveSelected_offline_explains() = runTest {
        online.value = false
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SelectionEvent.MoveSelected)

            assertEquals(RemoteBrowserEffect.Offline(RemoteOfflineAction.MOVE), awaitItem())
        }
    }

    @Test
    fun sheet_readOnlyFolder_isMarkedAndTheCurrentFolderChecked() {
        val readOnly = RemoteItem("/ro", "ro", true, permissions = "RG")
        val self = RemoteItem("/", "", true, permissions = "RG")
        every { operations.canWrite(readOnly) } returns false
        every { operations.canWrite(self) } returns false
        coEvery { operations.listFolder("/") } returns AppResult.Success(RemoteFolderContent(self, listOf(readOnly)))
        val viewModel = viewModel()

        viewModel.openMoveSheet()

        val sheet = checkNotNull(viewModel.state.value.move.sheet)
        assertEquals(listOf(MoveFolder(readOnly, canWrite = false)), sheet.folders)
        assertFalse(sheet.canWriteHere)
        assertFalse(sheet.canMoveHere)
    }

    @Test
    fun sheet_imagesOwnFolder_cannotBeTheTarget() {
        val viewModel = viewModel()
        viewModel.openMoveSheet()

        viewModel.onEvent(MoveEvent.SheetFolderOpened(IMAGE_A.parentPath))

        val sheet = checkNotNull(viewModel.state.value.move.sheet)
        assertTrue(sheet.isSourceFolder)
        assertFalse(sheet.canMoveHere)
    }

    @Test
    fun sheet_listFails_showsTheErrorAndCannotMove() {
        coEvery { operations.listFolder("/") } returns AppResult.Failure(AppError.NotFound)
        val viewModel = viewModel()

        viewModel.openMoveSheet()

        val sheet = checkNotNull(viewModel.state.value.move.sheet)
        assertEquals(AppError.NotFound, sheet.error)
        assertFalse(sheet.canMoveHere)
    }

    @Test
    fun sheetDismissed_closes() {
        val viewModel = viewModel()
        viewModel.openMoveSheet()

        viewModel.onEvent(MoveEvent.SheetDismissed)

        assertNull(viewModel.state.value.move.sheet)
    }

    @Test
    fun newFolder_requestAndDismiss_togglesTheDialog() {
        val viewModel = viewModel()
        viewModel.openMoveSheet()

        viewModel.onEvent(MoveEvent.NewFolderRequested)
        assertEquals(true, viewModel.state.value.move.sheet?.showNewFolder)

        viewModel.onEvent(MoveEvent.NewFolderDismissed)
        assertEquals(false, viewModel.state.value.move.sheet?.showNewFolder)
    }

    @Test
    fun newFolder_confirmed_createsAndOpensIt() {
        coEvery { operations.createFolder("/", "New") } returns AppResult.Success("/New")
        val viewModel = viewModel()
        viewModel.openMoveSheet()

        viewModel.onEvent(MoveEvent.NewFolderConfirmed("New"))

        assertEquals("/New", viewModel.state.value.move.sheet?.path)
    }

    @Test
    fun newFolder_fails_reportsTheError() = runTest {
        coEvery { operations.createFolder(any(), any()) } returns AppResult.Failure(AppError.Conflict)
        val viewModel = viewModel()
        viewModel.openMoveSheet()

        viewModel.effects.test {
            viewModel.onEvent(MoveEvent.NewFolderConfirmed("New"))

            assertEquals(RemoteBrowserEffect.Failed(AppError.Conflict), awaitItem())
        }
    }

    private fun RemoteBrowserViewModel.moveToItaly() {
        openMoveSheet()
        onEvent(MoveEvent.SheetFolderOpened("/P/Italy"))
        onEvent(MoveEvent.MoveHere)
    }

    private fun moving(result: RemoteMoveResult, policy: ConflictPolicy = ConflictPolicy.ASK) {
        coEvery { operations.move(listOf(IMAGE_A), "/P/Italy", policy) } returns listOf(result)
    }

    @Test
    fun moveHere_movesAskingAboutConflicts() = runTest {
        moving(RemoteMoveResult.Moved(IMAGE_A, "/P/Italy/a.jpg"))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.moveToItaly()

            assertEquals(RemoteBrowserEffect.Moved(1, "/P/Italy"), awaitItem())
        }
        coVerify { operations.move(listOf(IMAGE_A), "/P/Italy", ConflictPolicy.ASK) }
        assertNull(viewModel.state.value.move.sheet)
    }

    @Test
    fun moveHere_finished_refreshesAndClearsTheSelection() {
        moving(RemoteMoveResult.Moved(IMAGE_A, "/P/Italy/a.jpg"))
        val viewModel = viewModel()
        viewModel.onEvent(SelectionEvent.StartSelecting)

        viewModel.moveToItaly()

        assertFalse(viewModel.state.value.selecting)
        verify(atLeast = 2) { browse.list("/") }
    }

    @Test
    fun moveHere_sourceFolder_doesNothing() {
        val viewModel = viewModel()
        viewModel.openMoveSheet()
        viewModel.onEvent(MoveEvent.SheetFolderOpened(IMAGE_A.parentPath))

        viewModel.onEvent(MoveEvent.MoveHere)

        coVerify(exactly = 0) { operations.move(any(), any(), any()) }
    }

    @Test
    fun moveHere_conflict_asks() {
        moving(RemoteMoveResult.Conflict(IMAGE_A, IMAGE_B))
        val viewModel = viewModel()

        viewModel.moveToItaly()

        assertEquals(MoveConflictState(IMAGE_A, IMAGE_B, "/P/Italy", 0), viewModel.state.value.move.conflict)
    }

    @Test
    fun conflictResolved_continuesWithTheAnswerAndClosesTheDialog() = runTest {
        moving(RemoteMoveResult.Conflict(IMAGE_A, IMAGE_B))
        moving(RemoteMoveResult.Renamed(IMAGE_A, "/P/Italy/a (2).jpg"), ConflictPolicy.KEEP_BOTH)
        val viewModel = viewModel()
        viewModel.moveToItaly()

        viewModel.effects.test {
            viewModel.onEvent(MoveEvent.ConflictResolved(ConflictPolicy.KEEP_BOTH, applyToAll = false))

            assertEquals(RemoteBrowserEffect.Moved(1, "/P/Italy"), awaitItem())
        }
        assertNull(viewModel.state.value.move.conflict)
    }

    @Test
    fun conflictSkipped_reportsNoMove() = runTest {
        moving(RemoteMoveResult.Conflict(IMAGE_A, IMAGE_B))
        moving(RemoteMoveResult.Skipped(IMAGE_A), ConflictPolicy.SKIP)
        val viewModel = viewModel()
        viewModel.moveToItaly()

        viewModel.effects.test {
            viewModel.onEvent(MoveEvent.ConflictResolved(ConflictPolicy.SKIP, applyToAll = false))

            expectNoEvents()
        }
    }

    @Test
    fun moveHere_failure_reportsTheError() = runTest {
        moving(RemoteMoveResult.Failed(IMAGE_A, AppError.Forbidden))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.moveToItaly()

            assertEquals(RemoteBrowserEffect.Failed(AppError.Forbidden), awaitItem())
        }
    }

    @Test
    fun undo_movesBackAndRefreshes() {
        val moved = RemoteMoveResult.Moved(IMAGE_A, "/P/Italy/a.jpg")
        moving(moved)
        coEvery { operations.undoMove(listOf(moved)) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
        val viewModel = viewModel()
        viewModel.moveToItaly()

        viewModel.onEvent(MoveEvent.Undo)

        coVerify { operations.undoMove(listOf(moved)) }
    }

    @Test
    fun undo_fails_reportsTheError() = runTest {
        moving(RemoteMoveResult.Moved(IMAGE_A, "/P/Italy/a.jpg"))
        coEvery { operations.undoMove(any()) } returns
            RemoteBatchResult(emptyList(), listOf(IMAGE_A to AppError.Conflict))
        val viewModel = viewModel()
        viewModel.moveToItaly()

        viewModel.effects.test {
            awaitItem()

            viewModel.onEvent(MoveEvent.Undo)

            assertEquals(RemoteBrowserEffect.Failed(AppError.Conflict), awaitItem())
        }
    }

    // Deleting

    private fun RemoteBrowserViewModel.deleteImageA() {
        onEvent(SelectionEvent.ItemLongPressed(IMAGE_A))
        onEvent(SelectionEvent.MenuDelete)
    }

    @Test
    fun menuDelete_deletesAndOffersUndo() = runTest {
        coEvery { operations.delete(listOf(IMAGE_A)) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.deleteImageA()

            assertEquals(RemoteBrowserEffect.Deleted(1), awaitItem())
        }
    }

    @Test
    fun menuDelete_offline_explainsAndDeletesNothing() = runTest {
        online.value = false
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.deleteImageA()

            assertEquals(RemoteBrowserEffect.Offline(RemoteOfflineAction.DELETE), awaitItem())
        }
        coVerify(exactly = 0) { operations.delete(any()) }
    }

    @Test
    fun menuDelete_fails_reportsTheErrorWithoutUndo() = runTest {
        coEvery { operations.delete(any()) } returns
            RemoteBatchResult(emptyList(), listOf(IMAGE_A to AppError.Forbidden))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.deleteImageA()

            assertEquals(RemoteBrowserEffect.Failed(AppError.Forbidden), awaitItem())
        }
    }

    @Test
    fun undoDelete_restoresTheDeletedImages() {
        coEvery { operations.delete(any()) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
        coEvery { operations.undoDelete(any()) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
        val viewModel = viewModel()
        viewModel.deleteImageA()

        viewModel.onEvent(ActionEvent.UndoDelete)

        coVerify { operations.undoDelete(listOf(IMAGE_A)) }
    }

    @Test
    fun undoDelete_secondTime_doesNothing() {
        coEvery { operations.delete(any()) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
        coEvery { operations.undoDelete(any()) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
        val viewModel = viewModel()
        viewModel.deleteImageA()
        viewModel.onEvent(ActionEvent.UndoDelete)

        viewModel.onEvent(ActionEvent.UndoDelete)

        coVerify(exactly = 1) { operations.undoDelete(any()) }
    }

    private fun trashBinOff() {
        coEvery { operations.isTrashBinEnabled() } returns false
        coEvery { operations.delete(any()) } returns RemoteBatchResult(listOf(IMAGE_A), emptyList())
    }

    @Test
    fun menuDelete_trashBinOff_asksInsteadOfDeleting() {
        trashBinOff()
        val viewModel = viewModel()

        viewModel.deleteImageA()

        assertEquals(DeleteConfirmationState(listOf(IMAGE_A)), viewModel.state.value.deleteConfirmation)
        coVerify(exactly = 0) { operations.delete(any()) }
    }

    @Test
    fun confirmDelete_deletesAndReportsItWithoutUndo() = runTest {
        trashBinOff()
        val viewModel = viewModel()
        viewModel.deleteImageA()

        viewModel.effects.test {
            viewModel.onEvent(ActionEvent.ConfirmDelete)

            assertEquals(RemoteBrowserEffect.DeletedPermanently(1), awaitItem())
        }
        assertNull(viewModel.state.value.deleteConfirmation)
    }

    @Test
    fun confirmDelete_undoAfterwards_restoresNothing() {
        trashBinOff()
        val viewModel = viewModel()
        viewModel.deleteImageA()
        viewModel.onEvent(ActionEvent.ConfirmDelete)

        viewModel.onEvent(ActionEvent.UndoDelete)

        coVerify(exactly = 0) { operations.undoDelete(any()) }
    }

    @Test
    fun cancelDelete_closesTheDialogAndDeletesNothing() {
        trashBinOff()
        val viewModel = viewModel()
        viewModel.deleteImageA()

        viewModel.onEvent(ActionEvent.CancelDelete)

        assertNull(viewModel.state.value.deleteConfirmation)
        coVerify(exactly = 0) { operations.delete(any()) }
    }

    @Test
    fun accountChanged_closesTheDeleteConfirmation() {
        trashBinOff()
        val viewModel = viewModel()
        viewModel.deleteImageA()

        urls.value = null

        assertNull(viewModel.state.value.deleteConfirmation)
    }

    // Sharing

    private fun RemoteBrowserViewModel.shareImageA() {
        onEvent(SelectionEvent.ItemLongPressed(IMAGE_A))
        onEvent(SelectionEvent.MenuShare)
    }

    @Test
    fun menuShare_showsProgress() {
        every { shares.prepare(listOf(IMAGE_A)) } returns MutableSharedFlow<RemoteShareProgress>(replay = 1).also {
            it.tryEmit(RemoteShareProgress.Downloading(2, 3))
        }
        val viewModel = viewModel()

        viewModel.shareImageA()

        assertEquals(RemoteShareUiState(2, 3), viewModel.state.value.share)
    }

    @Test
    fun menuShare_ready_opensTheShareSheet() = runTest {
        val spec = ShareSpec(listOf("content://a"), "image/jpeg")
        every { shares.prepare(any()) } returns flowOf(RemoteShareProgress.Ready(spec))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.shareImageA()

            assertEquals(RemoteBrowserEffect.Share(spec), awaitItem())
        }
        assertNull(viewModel.state.value.share)
    }

    @Test
    fun menuShare_noConnection_explains() = runTest {
        every { shares.prepare(any()) } returns flowOf(RemoteShareProgress.Failed(AppError.Network(null)))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.shareImageA()

            assertEquals(RemoteBrowserEffect.Offline(RemoteOfflineAction.SHARE), awaitItem())
        }
    }

    @Test
    fun menuShare_otherFailure_reportsTheError() = runTest {
        every { shares.prepare(any()) } returns flowOf(RemoteShareProgress.Failed(AppError.NotFound))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.shareImageA()

            assertEquals(RemoteBrowserEffect.Failed(AppError.NotFound), awaitItem())
        }
    }

    @Test
    fun cancelShare_stopsAndCleansUp() {
        every { shares.prepare(any()) } returns MutableSharedFlow<RemoteShareProgress>(replay = 1).also {
            it.tryEmit(RemoteShareProgress.Downloading(1, 3))
        }
        val viewModel = viewModel()
        viewModel.shareImageA()

        viewModel.onEvent(ActionEvent.CancelShare)

        assertNull(viewModel.state.value.share)
        verify { shares.clear() }
    }
}
