package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.first
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.nextcloud.NextcloudErrorMapper
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Every remote call goes through here. Without an account nothing is sent (D35), and exceptions of the client become
 * [AppError]s.
 */
class RemoteCallRunner @Inject constructor(
    private val accounts: AccountStore,
    private val localNetwork: LocalNetworkPolicy,
) {
    suspend fun currentAccount(): NextcloudAccount? = accounts.observeAccount().first()

    suspend fun <T> call(block: suspend (NextcloudAccount) -> T): AppResult<T> {
        val account = currentAccount() ?: return AppResult.Failure(AppError.NotLoggedIn)
        return call(account, block)
    }

    /** A call on behalf of [owner]. If another account, or none, is signed in when it ends, its result is gone. */
    suspend fun <T> call(owner: NextcloudAccount, block: suspend (NextcloudAccount) -> T): AppResult<T> {
        val result = if (isSignedIn(owner)) run(owner, block) else AppResult.Failure(AppError.NotLoggedIn)
        return if (isSignedIn(owner)) result else AppResult.Failure(AppError.NotLoggedIn)
    }

    private suspend fun isSignedIn(owner: NextcloudAccount) = currentAccount()?.isSameUserAs(owner) == true

    private suspend fun <T> run(account: NextcloudAccount, block: suspend (NextcloudAccount) -> T): AppResult<T> = try {
        AppResult.Success(block(account))
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        val host = account.serverUrl.toHttpUrlOrNull()?.host.orEmpty()
        AppResult.Failure(localNetwork.refine(NextcloudErrorMapper.map(e), host))
    }
}
