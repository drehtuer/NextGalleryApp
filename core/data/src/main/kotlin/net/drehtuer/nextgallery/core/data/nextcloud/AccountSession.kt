package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.data.upload.UploadStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import javax.inject.Inject
import javax.inject.Singleton

/** A new app password of the same user is still the same account: the local data stays valid. */
internal fun NextcloudAccount.isSameUserAs(other: NextcloudAccount): Boolean =
    serverUrl == other.serverUrl && userId == other.userId

/** Everything of the account that is kept locally, apart from the account itself (D35). */
class RemoteDataWiper @Inject constructor(
    private val cache: RemoteCacheStore,
    private val images: RemoteImageCache,
    private val shares: RemoteShareCache,
    private val connections: ConnectionEvictor,
    private val uploadStates: UploadStateStore,
) {
    suspend fun wipe() {
        val steps = listOf<suspend () -> Unit>(
            { cache.clear() },
            { images.clear() },
            { shares.clear() },
            { connections.evictConnections() },
            { uploadStates.clear() },
        )
        steps.forEach { step -> ignoringFailure(step) }
    }
}

/**
 * One step failing must not keep the others from running, and that includes a step that cancels itself. So this may
 * only run where the caller can't be cancelled: in [NonCancellable], or in the scope of [AppPasswordRevoker].
 */
private suspend fun ignoringFailure(step: suspend () -> Unit) {
    try {
        step()
    } catch (
        @Suppress("TooGenericExceptionCaught", "SwallowedException") _: Exception,
    ) {
        // Best effort.
    }
}

/** Ends app passwords the app no longer uses, best effort. [scope] outlives the screens: closing one can't stop it. */
class AppPasswordRevoker(private val ocs: OcsClient, private val scope: CoroutineScope) {
    /** The credentials go on the request, so this also works when the account store no longer has them. */
    fun revoke(serverUrl: String, loginName: String, appPassword: String) {
        scope.launch { ignoringFailure { ocs.revokeAppPassword(serverUrl, loginName, appPassword) } }
    }
}

/**
 * Changes of the signed-in account, and writes of data that belongs to an account. They exclude each other, so a write
 * either happens before the wipe that removes it or sees that its account is gone. Changes are not cancellable: half of
 * a logout is worse than a screen that closed.
 */
@Singleton
class AccountSession @Inject constructor(
    private val accounts: AccountStore,
    private val wiper: RemoteDataWiper,
    private val revoker: AppPasswordRevoker,
) {
    private val lock = Mutex()

    /** Runs [write] only while [owner] is still signed in, and returns whether it ran. */
    suspend fun writeFor(owner: NextcloudAccount, write: suspend () -> Unit): Boolean = lock.withLock {
        val isCurrent = accounts.observeAccount().first()?.isSameUserAs(owner) == true
        if (isCurrent) write()
        isCurrent
    }

    /** Local data can only stay if it provably is the data of this user; after a logout or a lost key it isn't. */
    suspend fun replaceAccount(account: NextcloudAccount) = exclusive {
        val previous = accounts.observeAccount().first()
        if (previous?.isSameUserAs(account) != true) wiper.wipe()
        accounts.save(account)
        if (previous != null && previous.appPassword != account.appPassword) {
            revoker.revoke(previous.serverUrl, previous.loginName, previous.appPassword)
        }
    }

    /** Ends the password on the server unless the signed-in account uses it. */
    suspend fun revokeIfUnused(serverUrl: String, loginName: String, appPassword: String) = exclusive {
        var inUse = false
        ignoringFailure { inUse = accounts.observeAccount().first()?.appPassword == appPassword }
        if (!inUse) revoker.revoke(serverUrl, loginName, appPassword)
    }

    /** Local first, each step regardless of the others. The server only hears about it afterwards (D35). */
    suspend fun logOut() = exclusive {
        var account: NextcloudAccount? = null
        ignoringFailure { account = accounts.observeAccount().first() }
        ignoringFailure { accounts.clear() }
        wiper.wipe()
        account?.let { revoker.revoke(it.serverUrl, it.loginName, it.appPassword) }
    }

    /**
     * What must survive a cancelled caller goes inside [change]: [withContext] finishes the block, but then throws into
     * a cancelled caller, so that code after this call would not run.
     */
    private suspend fun exclusive(change: suspend () -> Unit) = withContext(NonCancellable) {
        lock.withLock { change() }
    }
}
