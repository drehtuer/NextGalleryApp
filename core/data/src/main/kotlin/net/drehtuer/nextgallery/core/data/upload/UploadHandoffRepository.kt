package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import net.drehtuer.nextgallery.core.data.nextcloud.AccountStore
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.NEXTCLOUD_APP_PACKAGE
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadHandoff
import net.drehtuer.nextgallery.core.model.UploadState
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/** The app never uploads itself: it only hands images to the Nextcloud app (D31). */
interface UploadHandoffRepository {
    /** Starts with [UploadAvailability.HIDDEN], so that a screen combining it does not wait for it. */
    fun observeAvailability(): Flow<UploadAvailability>

    /** Leaves out the images already on the server. Checks for the Nextcloud app again: it may have been installed. */
    suspend fun prepare(ids: Collection<MediaId>): UploadHandoff

    /**
     * To be called whenever a screen resumes. Looks for the Nextcloud app again and, on returning from a hand-off,
     * requests an upload-state check.
     */
    fun onAppResumed()
}

class DefaultUploadHandoffRepository @Inject constructor(
    private val accounts: AccountStore,
    private val appSource: NextcloudAppSource,
    private val indexStore: MediaIndexStore,
    private val stateStore: UploadStateStore,
    private val workScheduler: WorkScheduler,
) : UploadHandoffRepository {
    private val appChecks = MutableStateFlow(0)
    private val handedOff = AtomicBoolean(false)

    override fun observeAvailability(): Flow<UploadAvailability> = combine(
        accounts.observeAccount().map { it != null }.distinctUntilChanged(),
        appChecks,
    ) { loggedIn, _ ->
        when {
            !loggedIn -> UploadAvailability.HIDDEN
            appSource.isInstalled() -> UploadAvailability.AVAILABLE
            else -> UploadAvailability.APP_MISSING
        }
    }.onStart { emit(UploadAvailability.HIDDEN) }.distinctUntilChanged()

    override suspend fun prepare(ids: Collection<MediaId>): UploadHandoff {
        if (!appSource.isInstalled()) return UploadHandoff.NextcloudAppMissing
        val states = stateStore.states(ids)
        val notUploaded = indexStore.getByIds(ids).filterNot { states[it.id] is UploadState.Uploaded }
        val spec = ShareSpec.from(notUploaded, NEXTCLOUD_APP_PACKAGE)
        if (spec != null) handedOff.set(true)
        return spec?.let { UploadHandoff.Ready(it) } ?: UploadHandoff.NothingToUpload
    }

    override fun onAppResumed() {
        appChecks.update { it + 1 }
        if (handedOff.getAndSet(false)) workScheduler.requestUploadStateSync()
    }
}
