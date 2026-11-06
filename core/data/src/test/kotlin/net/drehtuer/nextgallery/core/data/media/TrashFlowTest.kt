package net.drehtuer.nextgallery.core.data.media

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.TrashEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrashFlowTest {
    private val ids = listOf(MediaId(1), MediaId(2))
    private val confirmation = PlatformConfirmation("trash")
    private val restoreConfirmation = PlatformConfirmation("restore")
    private val actions = mockk<MediaActionsRepository>(relaxed = true) {
        coEvery { delete(ids) } returns DeleteOutcome.NeedsUserConfirmation(confirmation, listOf("uri"))
        coEvery { restore(listOf("uri")) } returns AppResult.Success(restoreConfirmation)
    }
    private val flow = TrashFlow(actions)

    @Test
    fun request_needsConfirmation_asksForIt() = runTest {
        assertEquals(TrashEffect.RequestConfirmation(confirmation), flow.request(ids))
    }

    @Test
    fun request_nothingToDelete_hasNoEffect() = runTest {
        coEvery { actions.delete(ids) } returns DeleteOutcome.NothingToDelete

        assertNull(flow.request(ids))
    }

    @Test
    fun request_failed_reportsFailure() = runTest {
        coEvery { actions.delete(ids) } returns DeleteOutcome.Failed(AppError.PermissionDenied)

        assertEquals(TrashEffect.Failed, flow.request(ids))
    }

    @Test
    fun onConfirmationResult_confirmed_updatesIndex() = runTest {
        flow.request(ids)

        flow.onConfirmationResult(confirmed = true)

        coVerify { actions.onDeleteConfirmed(ids) }
    }

    @Test
    fun onConfirmationResult_confirmed_reportsCount() = runTest {
        flow.request(ids)

        assertEquals(TrashEffect.MovedToTrash(2), flow.onConfirmationResult(confirmed = true))
    }

    @Test
    fun onConfirmationResult_declined_keepsIndex() = runTest {
        flow.request(ids)

        flow.onConfirmationResult(confirmed = false)

        coVerify(exactly = 0) { actions.onDeleteConfirmed(any()) }
    }

    @Test
    fun onConfirmationResult_withoutRequest_hasNoEffect() = runTest {
        assertNull(flow.onConfirmationResult(confirmed = true))
    }

    @Test
    fun undo_afterTrash_asksForRestoreConfirmation() = runTest {
        flow.request(ids)
        flow.onConfirmationResult(confirmed = true)

        assertEquals(TrashEffect.RequestConfirmation(restoreConfirmation), flow.undo())
    }

    @Test
    fun undo_nothingTrashed_hasNoEffect() = runTest {
        assertNull(flow.undo())
    }

    @Test
    fun undo_requestFails_reportsFailure() = runTest {
        coEvery { actions.restore(any()) } returns AppResult.Failure(AppError.PermissionDenied)
        flow.request(ids)
        flow.onConfirmationResult(confirmed = true)

        assertEquals(TrashEffect.Failed, flow.undo())
    }

    @Test
    fun onConfirmationResult_afterUndo_doesNotDeleteAgain() = runTest {
        flow.request(ids)
        flow.onConfirmationResult(confirmed = true)
        flow.undo()

        flow.onConfirmationResult(confirmed = true)

        coVerify(exactly = 1) { actions.onDeleteConfirmed(any()) }
    }
}
