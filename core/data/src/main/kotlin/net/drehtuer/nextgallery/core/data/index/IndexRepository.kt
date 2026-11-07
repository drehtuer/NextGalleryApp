package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton

interface IndexRepository {
    val status: StateFlow<IndexStatus>

    suspend fun sync(mode: IndexMode): AppResult<IndexStats>
}

@Singleton
class DefaultIndexRepository @Inject constructor(
    private val indexer: MediaIndexer,
    private val locationBackfill: LocationBackfill,
    private val dispatchers: DispatcherProvider,
) : IndexRepository {
    private val mutableStatus = MutableStateFlow<IndexStatus>(IndexStatus.Idle)
    override val status: StateFlow<IndexStatus> = mutableStatus.asStateFlow()

    override suspend fun sync(mode: IndexMode): AppResult<IndexStats> {
        mutableStatus.value = IndexStatus.Running
        try {
            val result = withContext(dispatchers.io) {
                runCatchingCancellable { indexer.sync(mode).also { locationBackfill.run() } }
            }
            mutableStatus.value = when (result) {
                is AppResult.Success -> IndexStatus.Idle
                is AppResult.Failure -> IndexStatus.Failed(result.error)
            }
            return result
        } finally {
            if (mutableStatus.value == IndexStatus.Running) mutableStatus.value = IndexStatus.Idle
        }
    }
}
