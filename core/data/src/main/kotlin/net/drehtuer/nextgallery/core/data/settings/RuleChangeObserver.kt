package net.drehtuer.nextgallery.core.data.settings

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import javax.inject.Inject

/**
 * Rebuilds the index when the folder rules change. It has to be a full scan: a rule change can bring back images that
 * an incremental sync would not look at. The debounce turns several quick changes into one scan.
 */
class RuleChangeObserver @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workScheduler: WorkScheduler,
) {
    @OptIn(FlowPreview::class)
    suspend fun observe() {
        settingsRepository.observeSettings()
            .map { it.folderRules to it.excludePatterns }
            .distinctUntilChanged()
            .drop(1)
            .debounce(DEBOUNCE_MILLIS)
            .collect { workScheduler.enqueueIndex(IndexMode.FULL) }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 2_000L
    }
}
