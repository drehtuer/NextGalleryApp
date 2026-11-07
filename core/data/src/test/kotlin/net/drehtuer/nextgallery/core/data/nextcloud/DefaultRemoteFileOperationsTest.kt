package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpException
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.RemoteFolder
import net.drehtuer.nextgallery.core.nextcloud.TrashItem
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultRemoteFileOperationsTest {
    private val webDav = mockk<WebDavClient>(relaxed = true)
    private val store = FakeAccountStore()
    private val ocs = mockk<OcsClient>()
    private val operations = DefaultRemoteFileOperations(testRunner(store), webDav, ocs)

    private val item = RemoteItem("/A/IMG_1.jpg", "IMG_1.jpg", false, fileId = 42)
    private val existing = RemoteItem("/B/IMG_1.jpg", "IMG_1.jpg", false)

    private fun targetExists() {
        coEvery { webDav.move(TEST_ACCOUNT, item.path, "/B/IMG_1.jpg", false) } throws NextcloudHttpException(412)
    }

    @Test
    fun move_free_movesWithoutOverwrite() = runTest {
        val results = operations.move(listOf(item), "/B")

        assertEquals(listOf<RemoteMoveResult>(RemoteMoveResult.Moved(item, "/B/IMG_1.jpg")), results)
        coVerify { webDav.move(TEST_ACCOUNT, item.path, "/B/IMG_1.jpg", false) }
    }

    @Test
    fun move_defaultPolicy_neverOverwrites() = runTest {
        targetExists()
        coEvery { webDav.stat(TEST_ACCOUNT, "/B/IMG_1.jpg") } returns existing

        operations.move(listOf(item), "/B")

        coVerify(exactly = 0) { webDav.move(any(), any(), any(), true) }
    }

    @Test
    fun move_alreadyInTheTarget_isSkippedWithoutRequest() = runTest {
        val results = operations.move(listOf(item), "/A")

        assertEquals(listOf<RemoteMoveResult>(RemoteMoveResult.Skipped(item)), results)
        coVerify(exactly = 0) { webDav.move(any(), any(), any(), any()) }
    }

    @Test
    fun move_ask_reportsConflictWithTheExistingFile() = runTest {
        targetExists()
        coEvery { webDav.stat(TEST_ACCOUNT, "/B/IMG_1.jpg") } returns existing

        val result = operations.move(listOf(item), "/B", ConflictPolicy.ASK).single()

        assertEquals(RemoteMoveResult.Conflict(item, existing), result)
    }

    @Test
    fun move_askAndExistingUnreadable_reportsConflictWithoutDetails() = runTest {
        targetExists()
        coEvery { webDav.stat(any(), any()) } throws NextcloudHttpException(500)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.ASK).single()

        assertEquals(RemoteMoveResult.Conflict(item, null), result)
    }

    @Test
    fun move_skip_changesNothing() = runTest {
        targetExists()

        val result = operations.move(listOf(item), "/B", ConflictPolicy.SKIP).single()

        assertEquals(RemoteMoveResult.Skipped(item), result)
        coVerify(exactly = 1) { webDav.move(any(), any(), any(), any()) }
    }

    @Test
    fun move_replace_overwritesTheFile() = runTest {
        targetExists()
        coEvery { webDav.stat(TEST_ACCOUNT, "/B/IMG_1.jpg") } returns existing

        val result = operations.move(listOf(item), "/B", ConflictPolicy.REPLACE).single()

        assertEquals(RemoteMoveResult.Replaced(item, "/B/IMG_1.jpg"), result)
        coVerify { webDav.move(TEST_ACCOUNT, item.path, "/B/IMG_1.jpg", true) }
    }

    @Test
    fun move_replaceAFolder_isRefused() = runTest {
        targetExists()
        coEvery { webDav.stat(TEST_ACCOUNT, "/B/IMG_1.jpg") } returns existing.copy(isDirectory = true)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.REPLACE).single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Conflict), result)
        coVerify(exactly = 0) { webDav.move(any(), any(), any(), true) }
    }

    @Test
    fun move_replaceAndStatFails_isFailedWithoutOverwriting() = runTest {
        targetExists()
        coEvery { webDav.stat(any(), any()) } throws NextcloudHttpException(500)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.REPLACE).single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Server(500)), result)
        coVerify(exactly = 0) { webDav.move(any(), any(), any(), true) }
    }

    @Test
    fun move_replaceFails_isFailed() = runTest {
        targetExists()
        coEvery { webDav.stat(TEST_ACCOUNT, "/B/IMG_1.jpg") } returns existing
        coEvery { webDav.move(any(), any(), any(), true) } throws NextcloudHttpException(403)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.REPLACE).single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Forbidden), result)
    }

    @Test
    fun move_keepBoth_movesUnderTheNextFreeName() = runTest {
        targetExists()
        coEvery { webDav.list(TEST_ACCOUNT, "/B") } returns RemoteFolder(null, listOf(existing))

        val result = operations.move(listOf(item), "/B", ConflictPolicy.KEEP_BOTH).single()

        assertEquals(RemoteMoveResult.Renamed(item, "/B/IMG_1 (2).jpg"), result)
        coVerify { webDav.move(TEST_ACCOUNT, item.path, "/B/IMG_1 (2).jpg", false) }
    }

    @Test
    fun move_keepBothAndNameTakenMeanwhile_triesTheNextName() = runTest {
        targetExists()
        coEvery { webDav.list(TEST_ACCOUNT, "/B") } returns RemoteFolder(null, listOf(existing))
        coEvery { webDav.move(TEST_ACCOUNT, item.path, "/B/IMG_1 (2).jpg", false) } throws NextcloudHttpException(412)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.KEEP_BOTH).single()

        assertEquals(RemoteMoveResult.Renamed(item, "/B/IMG_1 (3).jpg"), result)
    }

    @Test
    fun move_keepBothNeverOverwrites() = runTest {
        targetExists()
        coEvery { webDav.list(TEST_ACCOUNT, "/B") } returns RemoteFolder(null, listOf(existing))

        operations.move(listOf(item), "/B", ConflictPolicy.KEEP_BOTH)

        coVerify(exactly = 0) { webDav.move(any(), any(), any(), true) }
    }

    @Test
    fun move_keepBothAndAllNamesTaken_isFailed() = runTest {
        coEvery { webDav.move(any(), any(), any(), false) } throws NextcloudHttpException(412)
        coEvery { webDav.list(TEST_ACCOUNT, "/B") } returns RemoteFolder(null, listOf(existing))

        val result = operations.move(listOf(item), "/B", ConflictPolicy.KEEP_BOTH).single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Conflict), result)
    }

    @Test
    fun move_keepBothAndListFails_isFailed() = runTest {
        targetExists()
        coEvery { webDav.list(any(), any()) } throws NextcloudHttpException(500)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.KEEP_BOTH).single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Server(500)), result)
    }

    @Test
    fun move_keepBothAndMoveFails_isFailed() = runTest {
        targetExists()
        coEvery { webDav.list(TEST_ACCOUNT, "/B") } returns RemoteFolder(null, listOf(existing))
        coEvery { webDav.move(TEST_ACCOUNT, item.path, "/B/IMG_1 (2).jpg", false) } throws NextcloudHttpException(403)

        val result = operations.move(listOf(item), "/B", ConflictPolicy.KEEP_BOTH).single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Forbidden), result)
    }

    @Test
    fun move_readOnlyTarget_isFailed() = runTest {
        coEvery { webDav.move(any(), any(), any(), any()) } throws NextcloudHttpException(403)

        val result = operations.move(listOf(item), "/B").single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.Forbidden), result)
    }

    @Test
    fun move_oneFails_theOthersStillMove() = runTest {
        val other = RemoteItem("/A/b.jpg", "b.jpg", false)
        coEvery { webDav.move(any(), item.path, any(), any()) } throws NextcloudHttpException(500)

        val results = operations.move(listOf(item, other), "/B")

        assertEquals(RemoteMoveResult.Moved(other, "/B/b.jpg"), results[1])
    }

    @Test
    fun move_loggedOut_failsWithoutNetwork() = runTest {
        store.account.value = null

        val result = operations.move(listOf(item), "/B").single()

        assertEquals(RemoteMoveResult.Failed(item, AppError.NotLoggedIn), result)
        coVerify(exactly = 0) { webDav.move(any(), any(), any(), any()) }
    }

    @Test
    fun undoMove_movesBackWithoutOverwrite() = runTest {
        val result = operations.undoMove(listOf(RemoteMoveResult.Moved(item, "/B/IMG_1.jpg")))

        assertEquals(listOf(item), result.done)
        coVerify { webDav.move(TEST_ACCOUNT, "/B/IMG_1.jpg", item.path, false) }
    }

    @Test
    fun undoMove_renamed_movesBackToTheOldName() = runTest {
        operations.undoMove(listOf(RemoteMoveResult.Renamed(item, "/B/IMG_1 (2).jpg")))

        coVerify { webDav.move(TEST_ACCOUNT, "/B/IMG_1 (2).jpg", item.path, false) }
    }

    @Test
    fun undoMove_replaced_movesBack() = runTest {
        operations.undoMove(listOf(RemoteMoveResult.Replaced(item, "/B/IMG_1.jpg")))

        coVerify { webDav.move(TEST_ACCOUNT, "/B/IMG_1.jpg", item.path, false) }
    }

    @Test
    fun undoMove_skippedAndFailed_doNothing() = runTest {
        val result = operations.undoMove(
            listOf(RemoteMoveResult.Skipped(item), RemoteMoveResult.Failed(item, AppError.Forbidden)),
        )

        assertTrue(result.done.isEmpty())
        coVerify(exactly = 0) { webDav.move(any(), any(), any(), any()) }
    }

    @Test
    fun undoMove_oldPlaceTaken_reportsFailure() = runTest {
        coEvery { webDav.move(any(), any(), any(), any()) } throws NextcloudHttpException(412)

        val result = operations.undoMove(listOf(RemoteMoveResult.Moved(item, "/B/IMG_1.jpg")))

        assertEquals(listOf(item to AppError.Conflict), result.failures)
    }

    @Test
    fun undoMove_goesBackwards() = runTest {
        val first = RemoteItem("/A/x.jpg", "x.jpg", false)
        val second = RemoteItem("/B/x.jpg", "x.jpg", false)

        operations.undoMove(
            listOf(RemoteMoveResult.Moved(first, "/C/x.jpg"), RemoteMoveResult.Replaced(second, "/C/x.jpg")),
        )

        coVerifyOrder {
            webDav.move(TEST_ACCOUNT, "/C/x.jpg", "/B/x.jpg", false)
            webDav.move(TEST_ACCOUNT, "/C/x.jpg", "/A/x.jpg", false)
        }
    }

    @Test
    fun delete_sendsDeleteForEachItem() = runTest {
        val result = operations.delete(listOf(item))

        assertEquals(listOf(item), result.done)
        coVerify { webDav.delete(TEST_ACCOUNT, item.path) }
    }

    @Test
    fun delete_failure_isReportedPerItem() = runTest {
        coEvery { webDav.delete(any(), any()) } throws NextcloudHttpException(403)

        val result = operations.delete(listOf(item))

        assertEquals(listOf(item to AppError.Forbidden), result.failures)
    }

    @Test
    fun delete_loggedOut_failsWithoutNetwork() = runTest {
        store.account.value = null

        val result = operations.delete(listOf(item))

        assertEquals(listOf(item to AppError.NotLoggedIn), result.failures)
        coVerify(exactly = 0) { webDav.delete(any(), any()) }
    }

    @Test
    fun isTrashBinEnabled_asksTheServerWithTheAccount() = runTest {
        coEvery { ocs.isTrashBinEnabled(TEST_ACCOUNT.serverUrl, "anna", "app-password") } returns true

        assertTrue(operations.isTrashBinEnabled())
    }

    @Test
    fun isTrashBinEnabled_answerIsKeptForTheAccount() = runTest {
        coEvery { ocs.isTrashBinEnabled(any(), any(), any()) } returns true
        operations.isTrashBinEnabled()

        operations.isTrashBinEnabled()

        coVerify(exactly = 1) { ocs.isTrashBinEnabled(any(), any(), any()) }
    }

    @Test
    fun isTrashBinEnabled_anotherAccount_asksAgain() = runTest {
        coEvery { ocs.isTrashBinEnabled(any(), any(), any()) } returns true
        operations.isTrashBinEnabled()
        store.account.value = TEST_ACCOUNT.copy(userId = "ben-id", loginName = "ben")
        coEvery { ocs.isTrashBinEnabled(any(), "ben", any()) } returns false

        assertFalse(operations.isTrashBinEnabled())
    }

    @Test
    fun isTrashBinEnabled_failure_isFalse() = runTest {
        coEvery { ocs.isTrashBinEnabled(any(), any(), any()) } throws IOException("invalid")

        assertFalse(operations.isTrashBinEnabled())
    }

    @Test
    fun isTrashBinEnabled_failure_isNotKept() = runTest {
        coEvery { ocs.isTrashBinEnabled(any(), any(), any()) } throws IOException("invalid")
        operations.isTrashBinEnabled()
        coEvery { ocs.isTrashBinEnabled(any(), any(), any()) } returns true

        assertTrue(operations.isTrashBinEnabled())
    }

    @Test
    fun isTrashBinEnabled_loggedOut_isFalseWithoutRequest() = runTest {
        store.account.value = null

        assertFalse(operations.isTrashBinEnabled())
        coVerify(exactly = 0) { ocs.isTrashBinEnabled(any(), any(), any()) }
    }

    private fun trashEntry(name: String, time: Long, fileId: Long? = null) =
        TrashItem(name, "IMG_1.jpg", item.path, time, fileId)

    @Test
    fun undoDelete_restoresTheEntryOfThatPath() = runTest {
        val entry = trashEntry("IMG_1.jpg.d5", 5)
        val unrelated = TrashItem("x.d9", "x", "/x", 9, null)
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(unrelated, entry)

        val result = operations.undoDelete(listOf(item))

        assertEquals(listOf(item), result.done)
        coVerify { webDav.restore(TEST_ACCOUNT, entry) }
    }

    @Test
    fun undoDelete_severalEntries_restoresTheNewest() = runTest {
        val older = trashEntry("IMG_1.jpg.d5", 5)
        val newer = trashEntry("IMG_1.jpg.d6", 6)
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(older, newer)

        operations.undoDelete(listOf(item))

        coVerify { webDav.restore(TEST_ACCOUNT, newer) }
    }

    @Test
    fun undoDelete_sameFileId_winsOverANewerEntry() = runTest {
        val same = trashEntry("IMG_1.jpg.d5", 5, fileId = 42)
        val newer = trashEntry("IMG_1.jpg.d6", 6, fileId = 7)
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(same, newer)

        operations.undoDelete(listOf(item))

        coVerify { webDav.restore(TEST_ACCOUNT, same) }
    }

    @Test
    fun undoDelete_samePathButAnotherFileId_reportsNotFoundAndRestoresNothing() = runTest {
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(trashEntry("IMG_1.jpg.d6", 6, fileId = 7))

        val result = operations.undoDelete(listOf(item))

        assertEquals(listOf(item to AppError.NotFound), result.failures)
        coVerify(exactly = 0) { webDav.restore(any(), any()) }
    }

    @Test
    fun undoDelete_itemWithoutFileId_usesTheNewestEntryOfThatPath() = runTest {
        val older = trashEntry("IMG_1.jpg.d5", 5, fileId = 7)
        val newer = trashEntry("IMG_1.jpg.d6", 6, fileId = 8)
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(older, newer)

        operations.undoDelete(listOf(item.copy(fileId = null)))

        coVerify { webDav.restore(TEST_ACCOUNT, newer) }
    }

    @Test
    fun undoDelete_entryWithoutFileId_isPreferredToNothingButNotToTheSameFile() = runTest {
        val unknown = trashEntry("IMG_1.jpg.d6", 6)
        val same = trashEntry("IMG_1.jpg.d5", 5, fileId = 42)
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(unknown, same)

        operations.undoDelete(listOf(item))

        coVerify { webDav.restore(TEST_ACCOUNT, same) }
    }

    @Test
    fun undoDelete_noEntry_reportsNotFound() = runTest {
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns emptyList()

        val result = operations.undoDelete(listOf(item))

        assertEquals(listOf(item to AppError.NotFound), result.failures)
    }

    @Test
    fun undoDelete_listFails_reportsAllItems() = runTest {
        coEvery { webDav.listTrash(any()) } throws NextcloudHttpException(500)

        val result = operations.undoDelete(listOf(item))

        assertEquals(listOf(item to AppError.Server(500)), result.failures)
    }

    @Test
    fun undoDelete_restoreFails_isReported() = runTest {
        coEvery { webDav.listTrash(TEST_ACCOUNT) } returns listOf(trashEntry("a", 5))
        coEvery { webDav.restore(any(), any()) } throws NextcloudHttpException(403)

        val result = operations.undoDelete(listOf(item))

        assertEquals(listOf(item to AppError.Forbidden), result.failures)
    }

    @Test
    fun listFolder_returnsSubfoldersByNameAndTheFolderItself() = runTest {
        val self = RemoteItem("/A", "A", true, permissions = "RGDNVCK")
        val b = RemoteItem("/A/b", "b", true)
        val a = RemoteItem("/A/A", "A", true)
        coEvery { webDav.list(TEST_ACCOUNT, "/A") } returns RemoteFolder(self, listOf(b, item, a))

        val result = operations.listFolder("/A")

        assertEquals(AppResult.Success(RemoteFolderContent(self, listOf(a, b))), result)
    }

    @Test
    fun listFolder_failure_isReturned() = runTest {
        coEvery { webDav.list(any(), any()) } throws NextcloudHttpException(404)

        assertEquals(AppResult.Failure(AppError.NotFound), operations.listFolder("/A"))
    }

    @Test
    fun createFolder_createsBelowTheParent() = runTest {
        assertEquals(AppResult.Success("/A/New"), operations.createFolder("/A", " New "))
        coVerify { webDav.createFolder(TEST_ACCOUNT, "/A/New") }
    }

    @Test
    fun createFolder_invalidName_sendsNothing() = runTest {
        assertEquals(AppResult.Failure(AppError.InvalidName), operations.createFolder("/A", "../x"))
        coVerify(exactly = 0) { webDav.createFolder(any(), any()) }
    }

    @Test
    fun createFolder_exists_isConflict() = runTest {
        coEvery { webDav.createFolder(any(), any()) } throws NextcloudHttpException(405)

        assertEquals(AppResult.Failure(AppError.Conflict), operations.createFolder("/A", "New"))
    }

    @Test
    fun canWrite_withC_isTrue() {
        assertTrue(operations.canWrite(RemoteItem("/A", "A", true, permissions = "RGDNVC")))
    }

    @Test
    fun canWrite_withK_isTrue() {
        assertTrue(operations.canWrite(RemoteItem("/A", "A", true, permissions = "RGDNVK")))
    }

    @Test
    fun canWrite_withoutCOrK_isFalse() {
        assertFalse(operations.canWrite(RemoteItem("/A", "A", true, permissions = "RGD")))
    }

    @Test
    fun canWrite_unknownPermissions_isTrue() {
        assertTrue(operations.canWrite(RemoteItem("/A", "A", true)))
    }

    @Test
    fun canWrite_unknownFolder_isTrue() {
        assertTrue(operations.canWrite(null))
    }

    @Test
    fun nextFreeName_addsTwo() {
        assertEquals("IMG_1 (2).jpg", operations.nextFreeName("IMG_1.jpg", setOf("IMG_1.jpg")))
    }

    @Test
    fun nextFreeName_skipsTakenNumbers() {
        assertEquals(
            "IMG_1 (3).jpg",
            operations.nextFreeName("IMG_1.jpg", setOf("IMG_1.jpg", "IMG_1 (2).jpg")),
        )
    }

    @Test
    fun nextFreeName_withoutExtension_appendsTheNumber() {
        assertEquals("README (2)", operations.nextFreeName("README", setOf("README")))
    }

    @Test
    fun nextFreeName_dotFile_isTreatedAsHavingNoExtension() {
        assertEquals(".hidden (2)", operations.nextFreeName(".hidden", setOf(".hidden")))
    }

    @Test
    fun nextFreeName_twoDots_keepsTheLastExtensionOnly() {
        assertEquals("a.tar (2).gz", operations.nextFreeName("a.tar.gz", setOf("a.tar.gz")))
    }
}
