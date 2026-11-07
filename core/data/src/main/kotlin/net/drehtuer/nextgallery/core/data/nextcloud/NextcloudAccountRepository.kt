package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowV2Client
import net.drehtuer.nextgallery.core.nextcloud.NextcloudErrorMapper
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.RemotePathMapper
import net.drehtuer.nextgallery.core.nextcloud.ServerUrlNormalizer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

sealed interface LoginState {
    /** Open [loginUrl] in the browser. */
    data class Started(val loginUrl: String) : LoginState

    data object Polling : LoginState

    data class Success(val account: NextcloudAccount) : LoginState

    data class Failed(val error: AppError) : LoginState
}

interface NextcloudAccountRepository {
    fun observeAccount(): Flow<NextcloudAccount?>

    /** Null while logged out. */
    fun observeImageUrls(): Flow<RemoteImageUrls?>

    /** Cancelling the collection cancels the login. */
    fun login(serverUrl: String): Flow<LoginState>

    /** Always ends logged out and with the local data gone, even if the server can't be reached (D35). */
    suspend fun logout()
}

/** Everything of the account that is kept locally, apart from the account itself (D35). */
class RemoteDataWiper @Inject constructor(
    private val cache: RemoteCacheStore,
    private val images: RemoteImageCache,
    private val shares: RemoteShareCache,
    private val connections: ConnectionEvictor,
) {
    suspend fun wipe() {
        val steps = listOf<suspend () -> Unit>(
            { cache.clear() },
            { images.clear() },
            { shares.clear() },
            { connections.evictConnections() },
        )
        steps.forEach { step -> ignoringFailure(step) }
    }
}

/** One step failing must not keep the others from running. */
private suspend fun ignoringFailure(step: suspend () -> Unit) {
    try {
        step()
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught", "SwallowedException") _: Exception,
    ) {
        // Best effort.
    }
}

class DefaultNextcloudAccountRepository @Inject constructor(
    private val normalizer: ServerUrlNormalizer,
    private val loginFlow: LoginFlowV2Client,
    private val ocs: OcsClient,
    private val accounts: AccountStore,
    private val wiper: RemoteDataWiper,
    private val localNetwork: LocalNetworkPolicy,
) : NextcloudAccountRepository {
    override fun observeAccount(): Flow<NextcloudAccount?> = accounts.observeAccount()

    override fun observeImageUrls(): Flow<RemoteImageUrls?> =
        accounts.observeAccount().map { account -> account?.let(::AccountImageUrls) }

    override fun login(serverUrl: String): Flow<LoginState> = flow {
        var host = ""
        try {
            val normalized = normalizer.normalize(serverUrl)
            host = normalized.toHttpUrlOrNull()?.host.orEmpty()
            val session = loginFlow.start(normalized)
            emit(LoginState.Started(session.loginUrl))
            emit(LoginState.Polling)
            val result = loginFlow.poll(session)
            val userId = ocs.userId(result.serverUrl, result.loginName, result.appPassword)
            val account = NextcloudAccount(result.serverUrl, result.loginName, userId, result.appPassword)
            replaceAccount(account)
            emit(LoginState.Success(account))
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            emit(LoginState.Failed(localNetwork.refine(NextcloudErrorMapper.map(e), host)))
        }
    }

    /** The cache of another account must not show under the new one. */
    private suspend fun replaceAccount(account: NextcloudAccount) {
        val previous = accounts.observeAccount().first()
        if (previous != null && (previous.serverUrl != account.serverUrl || previous.userId != account.userId)) {
            wiper.wipe()
        }
        accounts.save(account)
    }

    override suspend fun logout() {
        val account = accounts.observeAccount().first()
        if (account != null) {
            ignoringFailure { ocs.revokeAppPassword(account.serverUrl, account.loginName, account.appPassword) }
        }
        accounts.clear()
        wiper.wipe()
    }
}

/** Holds the account only to build URLs; nothing here exposes the password, and its [toString] hides it. */
private data class AccountImageUrls(private val account: NextcloudAccount) : RemoteImageUrls {
    override fun preview(item: RemoteItem, sizePx: Int): String? {
        val fileId = item.fileId?.takeIf { item.hasPreview } ?: return null
        return RemotePathMapper.previewUrl(account, fileId, sizePx).toString()
    }

    override fun original(item: RemoteItem): String = RemotePathMapper.davUrl(account, item.path).toString()
}
