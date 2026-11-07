package net.drehtuer.nextgallery.feature.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.drehtuer.nextgallery.core.model.RemoteItem

/** The remote selection and the long-press menu (D47). Separate from the app-wide local selection (D33). */
internal class RemoteSelectionController(
    private val browser: MutableStateFlow<RemoteBrowserUiState>,
    private val sendEffect: (RemoteBrowserEffect) -> Unit,
    private val startMove: (List<RemoteItem>) -> Unit,
    private val share: (List<RemoteItem>) -> Unit,
    private val delete: (List<RemoteItem>) -> Unit,
) {
    fun onEvent(event: SelectionEvent) {
        when (event) {
            is SelectionEvent.ItemClicked -> onItemClicked(event.item)

            is SelectionEvent.ItemLongPressed -> onItemLongPressed(event.item)

            SelectionEvent.StartSelecting -> browser.update { it.copy(selecting = true) }

            SelectionEvent.SelectAll -> browser.update { state ->
                state.copy(selection = state.selection + state.images.associateBy { it.path })
            }

            SelectionEvent.ClearSelection -> clear()

            SelectionEvent.MenuDismissed -> browser.update { it.copy(menu = null) }

            SelectionEvent.MenuMove -> withMenuItems(RemoteOfflineAction.MOVE, startMove)

            SelectionEvent.MenuShare -> withMenuItems(RemoteOfflineAction.SHARE, share)

            SelectionEvent.MenuDelete -> withMenuItems(RemoteOfflineAction.DELETE, delete)

            SelectionEvent.MoveSelected -> whenOnline(RemoteOfflineAction.MOVE) {
                startMove(browser.value.selection.values.toList())
            }
        }
    }

    fun clear() {
        browser.update { it.copy(selection = emptyMap(), selecting = false, menu = null) }
    }

    private fun onItemClicked(item: RemoteItem) {
        if (browser.value.selecting) {
            browser.update { state ->
                val selection = if (item.path in state.selection) {
                    state.selection - item.path
                } else {
                    state.selection + (item.path to item)
                }
                state.copy(selection = selection)
            }
        } else {
            sendEffect(RemoteBrowserEffect.OpenViewer(item.path))
        }
    }

    /** During a selection a long-press adds the image, and the menu then acts on the whole selection (D47). */
    private fun onItemLongPressed(item: RemoteItem) {
        browser.update { state ->
            if (state.selecting) {
                val selection = state.selection + (item.path to item)
                state.copy(selection = selection, menu = RemoteMenuState(item.path, selection.values.toList()))
            } else {
                state.copy(menu = RemoteMenuState(item.path, listOf(item)))
            }
        }
    }

    private fun withMenuItems(action: RemoteOfflineAction, block: (List<RemoteItem>) -> Unit) {
        val items = browser.value.menu?.items ?: return
        browser.update { it.copy(menu = null) }
        whenOnline(action) { block(items) }
    }

    private fun whenOnline(action: RemoteOfflineAction, block: () -> Unit) {
        if (browser.value.isOffline) sendEffect(RemoteBrowserEffect.Offline(action)) else block()
    }
}
