package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.data.upload.UploadStateStore
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.LoginExpiredException
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowResult
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowV2Client
import net.drehtuer.nextgallery.core.nextcloud.NextcloudErrorMapper
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.RemotePathMapper
import net.drehtuer.nextgallery.core.nextcloud.ServerMismatchException
import net.drehtuer.nextgallery.core.nextcloud.ServerUrlNormalizer
import net.drehtuer.nextgallery.core.nextcloud.isSameOrigin
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** A login waiting for the user in the browser. Holds the poll token, so [toString] hides it. */
data class PendingLogin(
    val serverUrl: String,
    val pollEndpoint: String,
    val token: String,
    val startedAtMillis: Long,
) {
    override fun toString(): String =
        "PendingLogin(serverUrl=$serverUrl, pollEndpoint=$pollEndpoint, token=<redacted>, startedAtMillis=$startedAtMillis)"
}

sealed interface LoginState {
    /**
     * Open [loginUrl] in the browser. [pending] lets the caller continue the login if the app is killed while the
     * user is in the browser.
     */
    data class Started(val loginUrl: String, val pending: PendingLogin) : LoginState

    data object Polling : LoginState

    data class Success(val account: NextcloudAccount) : LoginState

    data class Failed(val error: AppError) : LoginState
}

interface NextcloudAccountRepository {
    fun observeAccount(): Flow<NextcloudAccount?>

    /** Null while logged out. */
    fun observeImageUrls(): Flow<RemoteImageUrls?>

    /** Cancelling the collection cancels the login. */
    fun login(serverUrl: String, foreground: Flow<Boolean>): Flow<LoginState>

    /** Continues a login started earlier, after the app was killed while the user was in the browser. */
    fun resumeLogin(pending: PendingLogin, foreground: Flow<Boolean>): Flow<LoginState>

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
    private val clock: Clock,
) : NextcloudAccountRepository {
    override fun observeAccount(): Flow<NextcloudAccount?> = accounts.observeAccount()

    override fun observeImageUrls(): Flow<RemoteImageUrls?> =
        accounts.observeAccount().map { account -> account?.let(::AccountImageUrls) }

    override fun login(serverUrl: String, foreground: Flow<Boolean>): Flow<LoginState> = flow {
        var host = ""
        reportingFailure({ host }) {
            val normalized = normalizer.normalize(serverUrl)
            host = normalized.toHttpUrlOrNull()?.host.orEmpty()
            val session = loginFlow.start(normalized)
            val pending = PendingLogin(normalized, session.pollEndpoint, session.token, clock.nowMillis())
            emit(LoginState.Started(session.loginUrl, pending))
            emit(LoginState.Polling)
            emit(LoginState.Success(signIn(normalized, awaitResult(pending, foreground))))
        }
    }

    override fun resumeLogin(pending: PendingLogin, foreground: Flow<Boolean>): Flow<LoginState> = flow {
        reportingFailure({ pending.serverUrl.toHttpUrlOrNull()?.host.orEmpty() }) {
            emit(LoginState.Polling)
            emit(LoginState.Success(signIn(pending.serverUrl, awaitResult(pending, foreground))))
        }
    }

    private suspend fun FlowCollector<LoginState>.reportingFailure(host: () -> String, login: suspend () -> Unit) {
        try {
            login()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            emit(LoginState.Failed(localNetwork.refine(NextcloudErrorMapper.map(e), host())))
        }
    }

    /**
     * Asks while the app is in the foreground, so right when the user returns from the browser, and not in the
     * background, where Android may freeze the app and cut its network. A failed attempt only means the next one has
     * to be tried; the login ends when the server refuses it, the time is up, or the user cancels.
     */
    private suspend fun awaitResult(pending: PendingLogin, foreground: Flow<Boolean>): LoginFlowResult {
        while (true) {
            foreground.first { it }
            if (clock.nowMillis() - pending.startedAtMillis >= LOGIN_TIMEOUT_MILLIS) throw LoginExpiredException()
            checkOnce(pending)?.let { return it }
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private suspend fun checkOnce(pending: PendingLogin): LoginFlowResult? = try {
        loginFlow.check(pending.pollEndpoint, pending.token)
    } catch (e: IOException) {
        if (isTemporary(NextcloudErrorMapper.map(e))) null else throw e
    }

    private fun isTemporary(error: AppError): Boolean =
        error is AppError.Network || (error is AppError.Server && error.code >= FIRST_SERVER_ERROR_CODE)

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

    internal companion object {
        const val POLL_INTERVAL_MILLIS = 2_000L
        const val LOGIN_TIMEOUT_MILLIS = 20 * 60 * 1_000L
        private const val FIRST_SERVER_ERROR_CODE = 500
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
