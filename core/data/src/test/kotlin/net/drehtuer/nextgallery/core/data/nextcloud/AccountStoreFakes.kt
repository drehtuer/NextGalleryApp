package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import java.security.GeneralSecurityException

/** Reverses the bytes, so that stored data differs from the plain text. */
internal class FakeCipher(var failDecrypt: Boolean = false) : SecretCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.reversedArray()

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        if (failDecrypt) throw GeneralSecurityException("Key lost")
        return ciphertext.reversedArray()
    }
}

internal class FakeAccountStore(account: NextcloudAccount? = TEST_ACCOUNT) : AccountStore {
    val account = MutableStateFlow(account)

    override fun observeAccount(): Flow<NextcloudAccount?> = account

    override suspend fun save(account: NextcloudAccount) {
        this.account.value = account
    }

    override suspend fun clear() {
        account.value = null
    }
}

internal val TEST_ACCOUNT = NextcloudAccount("https://cloud.example.com", "anna", "anna-id", "app-password")

internal fun testRunner(store: AccountStore = FakeAccountStore()) =
    RemoteCallRunner(store, LocalNetworkPolicy(GrantedLocalNetwork) { emptyList() })

internal object GrantedLocalNetwork : LocalNetworkPermissionSource {
    override fun isGranted() = true
}
