package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.data.nextcloud.AccountStore
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteCallRunner
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.UploadState
import net.drehtuer.nextgallery.core.nextcloud.RemoteFolder
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import javax.inject.Inject

interface UploadStateRepository {
    /**
     * The badges of [visibleIds]: every id gets an entry, [UploadState.Unknown] if there is no valid record. Logged
     * out, the map is empty, so no badges are shown (D35). Reads only the cache, never the network, and starts with an
     * empty map, so that a screen combining it does not wait for it.
     */
    fun observeBadges(visibleIds: Flow<Set<MediaId>>): Flow<Map<MediaId, UploadState>>

    /**
     * Compares all indexed folders with the server, one `PROPFIND` per folder, and caches the result. Logged out, it
     * does nothing and succeeds. Results of finished folders are kept when a later folder fails.
     */
    suspend fun refreshAll(): AppResult<Unit>
}

class DefaultUploadStateRepository @Inject constructor(
    private val accounts: AccountStore,
    private val runner: RemoteCallRunner,
    private val webDav: WebDavClient,
    private val settings: SettingsRepository,
    private val store: UploadStateStore,
    private val checksums: LocalChecksums,
    private val clock: Clock,
) : UploadStateRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBadges(visibleIds: Flow<Set<MediaId>>): Flow<Map<MediaId, UploadState>> =
        accounts.observeAccount()
            .map { it != null }
            .distinctUntilChanged()
            .flatMapLatest { loggedIn ->
                if (loggedIn) visibleIds.flatMapLatest(::observeAllOf) else flowOf(emptyMap())
            }
            .onStart { emit(emptyMap()) }
            .distinctUntilChanged()

    private fun observeAllOf(ids: Set<MediaId>): Flow<Map<MediaId, UploadState>> =
        store.observeStates(ids).map { known -> ids.associateWith { known[it] ?: UploadState.Unknown } }

    override suspend fun refreshAll(): AppResult<Unit> {
        val account = accounts.observeAccount().first() ?: return AppResult.Success(Unit)
        val outcome = runCatchingCancellable {
            val remoteRoot = settings.observeSettings().first().remoteRoot
            store.localFolders().firstNotNullOfOrNull { refreshFolder(account, it, remoteRoot) }
        }
        return when (outcome) {
            is AppResult.Failure -> outcome

            is AppResult.Success -> when (val error = outcome.value) {
                null, AppError.NotLoggedIn -> AppResult.Success(Unit)
                else -> AppResult.Failure(error)
            }
        }
    }

    /** Returns the error that ends the whole check, or null to go on with the next folder. */
    private suspend fun refreshFolder(account: NextcloudAccount, relativePath: String, remoteRoot: String): AppError? {
        val path = RemoteTargetResolver.remoteFolderFor(relativePath, remoteRoot)
        return when (val result = runner.call { webDav.list(it, path) }) {
            is AppResult.Success -> compareFolder(account, relativePath, remoteRoot, result.value)

            is AppResult.Failure -> when {
                result.error == AppError.NotFound ->
                    compareFolder(account, relativePath, remoteRoot, RemoteFolder(null, emptyList()))

                result.error.endsTheCheck() -> result.error

                else -> null
            }
        }
    }

    private suspend fun compareFolder(
        account: NextcloudAccount,
        relativePath: String,
        remoteRoot: String,
        listing: RemoteFolder,
    ): AppError? {
        val items = store.itemsInFolder(relativePath)
        val previous = store.recordsInFolder(relativePath).associateBy { it.mediaId }
        val folderEtag = listing.self?.etag
        val unchanged =
            folderEtag != null && items.all { previous[it.id]?.isCurrent(it, remoteRoot, folderEtag) == true }
        if (unchanged) return null
        val records = UploadStateMatcher.match(
            items,
            listing.children,
            FolderComparison(remoteRoot, folderEtag, previous, clock.nowMillis()),
        ) { item, algorithm -> checksums.digest(item.contentUri, algorithm) }
        // A logout or a new account during the check must not get this account's results (D35).
        val sameAccount = accounts.observeAccount().first() == account
        if (sameAccount) store.upsertAll(records)
        return AppError.NotLoggedIn.takeUnless { sameAccount }
    }

    /** Skip rule: neither the server folder nor this image changed since the record was written. */
    private fun UploadStateRecord.isCurrent(item: MediaItem, remoteRoot: String, folderEtag: String) =
        remoteFolderEtag == folderEtag &&
            remotePath == RemoteTargetResolver.remotePathFor(item, remoteRoot) &&
            localSize == item.sizeBytes &&
            localDateModifiedSeconds == item.dateModifiedSeconds

    /** Other errors, such as no permission for one folder, concern only that folder. */
    private fun AppError.endsTheCheck() = when (this) {
        is AppError.Network,
        is AppError.Io,
        is AppError.Server,
        is AppError.UntrustedCertificate,
        AppError.Auth,
        AppError.LocalNetworkBlocked,
        AppError.NotLoggedIn,
        -> true

        else -> false
    }
}
