package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.RemoteItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteMoveFlowTest {
    private val operations = mockk<RemoteFileOperations>()
    private val flow = RemoteMoveFlow(operations)

    private val a = RemoteItem("/A/a.jpg", "a.jpg", false)
    private val b = RemoteItem("/A/b.jpg", "b.jpg", false)
    private val existing = RemoteItem("/B/a.jpg", "a.jpg", false)

    private fun moves(item: RemoteItem, policy: ConflictPolicy, result: RemoteMoveResult) {
        coEvery { operations.move(listOf(item), "/B", policy) } returns listOf(result)
    }

    private fun movedTo(item: RemoteItem) = RemoteMoveResult.Moved(item, "/B/${item.name}")

    @Test
    fun start_noConflicts_movesAllWithAsk() = runTest {
        moves(a, ConflictPolicy.ASK, movedTo(a))
        moves(b, ConflictPolicy.ASK, movedTo(b))

        val step = flow.start(listOf(a, b), "/B")

        assertEquals(RemoteMoveStep.Finished(listOf(movedTo(a), movedTo(b))), step)
    }

    @Test
    fun start_conflict_stopsAndAsks() = runTest {
        moves(a, ConflictPolicy.ASK, RemoteMoveResult.Conflict(a, existing))

        val step = flow.start(listOf(a, b), "/B")

        assertEquals(RemoteMoveStep.Conflict(a, existing, remaining = 1), step)
        coVerify(exactly = 0) { operations.move(listOf(b), any(), any()) }
    }

    @Test
    fun resolve_usesTheAnswerForThisImageAndContinues() = runTest {
        moves(a, ConflictPolicy.ASK, RemoteMoveResult.Conflict(a, existing))
        moves(a, ConflictPolicy.KEEP_BOTH, RemoteMoveResult.Renamed(a, "/B/a (2).jpg"))
        moves(b, ConflictPolicy.ASK, movedTo(b))
        flow.start(listOf(a, b), "/B")

        val step = flow.resolve(ConflictPolicy.KEEP_BOTH, applyToAll = false)

        assertEquals(RemoteMoveStep.Finished(listOf(RemoteMoveResult.Renamed(a, "/B/a (2).jpg"), movedTo(b))), step)
    }

    @Test
    fun resolve_withoutApplyToAll_asksAgainForTheNextConflict() = runTest {
        moves(a, ConflictPolicy.ASK, RemoteMoveResult.Conflict(a, existing))
        moves(a, ConflictPolicy.SKIP, RemoteMoveResult.Skipped(a))
        moves(b, ConflictPolicy.ASK, RemoteMoveResult.Conflict(b, null))
        flow.start(listOf(a, b), "/B")

        val step = flow.resolve(ConflictPolicy.SKIP, applyToAll = false)

        assertEquals(RemoteMoveStep.Conflict(b, null, remaining = 0), step)
    }

    @Test
    fun resolve_withApplyToAll_usesTheAnswerForTheRest() = runTest {
        moves(a, ConflictPolicy.ASK, RemoteMoveResult.Conflict(a, existing))
        moves(a, ConflictPolicy.REPLACE, RemoteMoveResult.Replaced(a, "/B/a.jpg"))
        moves(b, ConflictPolicy.REPLACE, RemoteMoveResult.Replaced(b, "/B/b.jpg"))
        flow.start(listOf(a, b), "/B")

        val step = flow.resolve(ConflictPolicy.REPLACE, applyToAll = true)

        assertEquals(
            RemoteMoveStep.Finished(
                listOf(RemoteMoveResult.Replaced(a, "/B/a.jpg"), RemoteMoveResult.Replaced(b, "/B/b.jpg")),
            ),
            step,
        )
    }

    @Test
    fun resolve_withoutAConflict_isAnError() {
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { flow.resolve(ConflictPolicy.SKIP, applyToAll = false) }
        }
    }

    @Test
    fun undo_movesBackTheResultsOfTheFinishedMove() = runTest {
        val results = listOf<RemoteMoveResult>(movedTo(a))
        moves(a, ConflictPolicy.ASK, movedTo(a))
        coEvery { operations.undoMove(results) } returns RemoteBatchResult(listOf(a), emptyList())
        flow.start(listOf(a), "/B")

        assertEquals(RemoteBatchResult(listOf(a), emptyList()), flow.undo())
    }

    @Test
    fun undo_secondTime_hasNothingToUndo() = runTest {
        moves(a, ConflictPolicy.ASK, movedTo(a))
        coEvery { operations.undoMove(any()) } returns RemoteBatchResult(emptyList(), emptyList())
        flow.start(listOf(a), "/B")
        flow.undo()

        flow.undo()

        coVerify { operations.undoMove(emptyList()) }
    }

    @Test
    fun start_failureOfOneImage_isPartOfTheResult() = runTest {
        val failed = RemoteMoveResult.Failed(a, AppError.Forbidden)
        moves(a, ConflictPolicy.ASK, failed)

        assertEquals(RemoteMoveStep.Finished(listOf(failed)), flow.start(listOf(a), "/B"))
    }
}
