package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowResult
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowV2Client
import net.drehtuer.nextgallery.core.nextcloud.NextcloudErrorMapper
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.RemotePathMapper
import net.drehtuer.nextgallery.core.nextcloud.ServerMismatchException
import net.drehtuer.nextgallery.core.nextcloud.ServerUrlNormalizer
import net.drehtuer.nextgallery.core.nextcloud.isSameOrigin
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

    /**
     * Always ends logged out and with the local data gone, even if the server can't be reached or the caller is
     * cancelled (D35). The server is told afterwards, in the background.
     */
    suspend fun logout()
}

class DefaultNextcloudAccountRepository @Inject constructor(
    private val normalizer: ServerUrlNormalizer,
    private val loginFlow: LoginFlowV2Client,
    private val ocs: OcsClient,
    private val accounts: AccountStore,
    private val session: AccountSession,
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
            emit(LoginState.Success(signIn(normalized, result)))
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            emit(LoginState.Failed(localNetwork.refine(NextcloudErrorMapper.map(e), host)))
        }
    }

    /**
     * The password exists on the server from here on, so it is ended again unless it ends up stored. Only the server
     * the user chose gets to see it: a different address in the response is not to be trusted with it.
     */
    private suspend fun signIn(entered: String, result: LoginFlowResult): NextcloudAccount {
        val isChosenServer = isSameOrigin(entered, result.serverUrl)
        try {
            if (!isChosenServer) throw ServerMismatchException()
            val userId = ocs.userId(result.serverUrl, result.loginName, result.appPassword)
            val account = NextcloudAccount(result.serverUrl, result.loginName, userId, result.appPassword)
            session.replaceAccount(account)
            return account
        } finally {
            val server = if (isChosenServer) result.serverUrl else entered
            session.revokeIfUnused(server, result.loginName, result.appPassword)
        }
    }

    override suspend fun logout() = session.logOut()
}

/** Holds the account only to build URLs; nothing here exposes the password, and its [toString] hides it. */
private data class AccountImageUrls(private val account: NextcloudAccount) : RemoteImageUrls {
    override fun preview(item: RemoteItem, sizePx: Int): String? {
        val fileId = item.fileId?.takeIf { item.hasPreview } ?: return null
        return RemotePathMapper.previewUrl(account, fileId, sizePx).toString()
    }

    override fun original(item: RemoteItem): String = RemotePathMapper.davUrl(account, item.path).toString()
}
