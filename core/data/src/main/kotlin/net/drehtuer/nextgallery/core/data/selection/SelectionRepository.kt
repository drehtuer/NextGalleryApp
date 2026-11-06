package net.drehtuer.nextgallery.core.data.selection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.drehtuer.nextgallery.core.model.MediaId
import javax.inject.Inject

/** App-wide selection of local images (D33). It lives in memory and does not survive process death. */
interface SelectionRepository {
    fun observe(): StateFlow<Set<MediaId>>

    fun toggle(id: MediaId)

    /** Adds everything between [from] and [to] (inclusive, in either direction); ignored if one is not in the list. */
    fun selectRange(orderedIds: List<MediaId>, from: MediaId, to: MediaId)

    /** Adds [ids] to the selection; images selected elsewhere stay selected. */
    fun selectAll(ids: Collection<MediaId>)

    fun clear()
}

class DefaultSelectionRepository @Inject constructor() : SelectionRepository {
    private val selection = MutableStateFlow<Set<MediaId>>(emptySet())

    override fun observe(): StateFlow<Set<MediaId>> = selection.asStateFlow()

    override fun toggle(id: MediaId) = selection.update { if (id in it) it - id else it + id }

    override fun selectRange(orderedIds: List<MediaId>, from: MediaId, to: MediaId) {
        val first = orderedIds.indexOf(from)
        val second = orderedIds.indexOf(to)
        if (first < 0 || second < 0) return
        selectAll(orderedIds.subList(minOf(first, second), maxOf(first, second) + 1))
    }

    override fun selectAll(ids: Collection<MediaId>) = selection.update { it + ids }

    override fun clear() = selection.update { emptySet() }
}
