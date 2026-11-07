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
    suspend fun <T> call(block: suspend (NextcloudAccount) -> T): AppResult<T> {
        val account = accounts.observeAccount().first() ?: return AppResult.Failure(AppError.NotLoggedIn)
        return try {
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
}
