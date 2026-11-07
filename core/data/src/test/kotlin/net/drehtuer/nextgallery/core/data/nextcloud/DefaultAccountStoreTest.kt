package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DefaultAccountStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = FakeCipher()
    private val file by lazy { File(folder.root, "account.preferences_pb") }
    private val store by lazy { DefaultAccountStore.create(file, cipher) }

    @Test
    fun observeAccount_nothingStored_isNull() = runTest {
        assertNull(store.observeAccount().first())
    }

    @Test
    fun save_thenObserve_returnsTheAccount() = runTest {
        store.save(TEST_ACCOUNT)

        assertEquals(TEST_ACCOUNT, store.observeAccount().first())
    }

    @Test
    fun save_doesNotWriteThePasswordInPlainText() = runTest {
        store.save(TEST_ACCOUNT)

        assertFalse(String(file.readBytes(), Charsets.ISO_8859_1).contains(TEST_ACCOUNT.appPassword))
    }

    @Test
    fun clear_removesTheAccount() = runTest {
        store.save(TEST_ACCOUNT)

        store.clear()

        assertNull(store.observeAccount().first())
    }

    @Test
    fun observeAccount_undecryptableData_isNull() = runTest {
        store.save(TEST_ACCOUNT)
        cipher.failDecrypt = true

        assertNull(store.observeAccount().first())
    }

    @Test
    fun observeAccount_decryptedButNotAnAccount_isNull() = runTest {
        val garbling = object : SecretCipher {
            override fun encrypt(plaintext: ByteArray) = plaintext

            override fun decrypt(ciphertext: ByteArray) = "{}".toByteArray()
        }
        val other = DefaultAccountStore.create(File(folder.root, "other.preferences_pb"), garbling)
        other.save(TEST_ACCOUNT)

        assertNull(other.observeAccount().first())
    }

    @Test
    fun observeAccount_corruptFile_isNull() = runTest {
        file.writeText("this is not a preferences file")

        assertNull(store.observeAccount().first())
    }

    @Test
    fun storedCredentialsProvider_returnsTheAccount() {
        val provider = StoredCredentialsProvider(FakeAccountStore())

        assertEquals(TEST_ACCOUNT, provider.current())
    }

    @Test
    fun storedCredentialsProvider_loggedOut_isNull() {
        assertNull(StoredCredentialsProvider(FakeAccountStore(null)).current())
    }
}
