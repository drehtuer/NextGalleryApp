package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.nextcloud.OcsClient

/**
 * Whether the server keeps deleted files (D56). A known answer is kept in memory for the account it belongs to; a
 * failed check is not, so the next delete asks again. Unknown counts as no.
 */
internal class TrashBinCheck(private val runner: RemoteCallRunner, private val ocs: OcsClient) {
    @Volatile
    private var kept: Answer? = null

    suspend fun isEnabled(): Boolean {
        val account = runner.currentAccount() ?: return false
        val answer = kept?.takeIf { it.serverUrl == account.serverUrl && it.userId == account.userId }
        return answer?.enabled ?: ask(account)
    }

    private suspend fun ask(account: NextcloudAccount): Boolean {
        val result = runner.call(account) { ocs.isTrashBinEnabled(it.serverUrl, it.loginName, it.appPassword) }
        return when (result) {
            is AppResult.Success -> {
                kept = Answer(account.serverUrl, account.userId, result.value)
                result.value
            }

            is AppResult.Failure -> false
        }
    }

    /** Whose answer it is, without the password, which is not needed to tell accounts apart. */
    private data class Answer(val serverUrl: String, val userId: String, val enabled: Boolean)
}
