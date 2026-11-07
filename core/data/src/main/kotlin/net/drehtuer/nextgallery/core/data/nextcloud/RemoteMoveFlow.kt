package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.model.RemoteItem
import javax.inject.Inject

sealed interface RemoteMoveStep {
    /** The user decides what happens to [item]. [remaining] is the number of other conflicts seen so far or to come. */
    data class Conflict(val item: RemoteItem, val existing: RemoteItem?, val remaining: Int) : RemoteMoveStep

    data class Finished(val results: List<RemoteMoveResult>) : RemoteMoveStep
}

/**
 * The steps of moving images: each image moves until one has a name conflict, the user decides, and it goes on. "Do the
 * same for the other conflicts" is kept until the end. Each ViewModel has its own instance.
 */
class RemoteMoveFlow @Inject constructor(private val operations: RemoteFileOperations) {
    private var target = ""
    private var pending = ArrayDeque<RemoteItem>()
    private var results = mutableListOf<RemoteMoveResult>()
    private var current: RemoteItem? = null
    private var appliedPolicy = ConflictPolicy.ASK
    private var lastFinished: List<RemoteMoveResult> = emptyList()

    suspend fun start(items: List<RemoteItem>, targetFolder: String): RemoteMoveStep {
        target = targetFolder
        pending = ArrayDeque(items)
        results = mutableListOf()
        current = null
        appliedPolicy = ConflictPolicy.ASK
        return advance()
    }

    /** The answer to [RemoteMoveStep.Conflict]; [applyToAll] uses it for the rest of this move without asking. */
    suspend fun resolve(policy: ConflictPolicy, applyToAll: Boolean): RemoteMoveStep {
        val item = checkNotNull(current) { "No conflict is waiting for an answer" }
        current = null
        if (applyToAll) appliedPolicy = policy
        results += operations.move(listOf(item), target, policy)
        return advance()
    }

    /** Moves everything back that the last finished move changed. Only once. */
    suspend fun undo(): RemoteBatchResult {
        val toUndo = lastFinished
        lastFinished = emptyList()
        return operations.undoMove(toUndo)
    }

    private suspend fun advance(): RemoteMoveStep {
        while (pending.isNotEmpty()) {
            val item = pending.removeFirst()
            when (val result = operations.move(listOf(item), target, appliedPolicy).single()) {
                is RemoteMoveResult.Conflict -> {
                    current = item
                    return RemoteMoveStep.Conflict(item, result.existing, remaining = pending.size)
                }

                else -> results += result
            }
        }
        lastFinished = results.toList()
        return RemoteMoveStep.Finished(lastFinished)
    }
}
