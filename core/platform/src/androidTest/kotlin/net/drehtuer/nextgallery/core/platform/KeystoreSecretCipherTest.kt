package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException

/** Robolectric has no AndroidKeyStore provider (ARCHITECTURE.md §13.1). */
@RunWith(AndroidJUnit4::class)
class KeystoreSecretCipherTest {
    private val cipher = KeystoreSecretCipher(ApplicationProvider.getApplicationContext<Context>())
    private val secret = "test secret".toByteArray()

    @Test
    fun encryptThenDecrypt_returnsThePlaintext() {
        assertArrayEquals(secret, cipher.decrypt(cipher.encrypt(secret)))
    }

    @Test
    fun encrypt_doesNotContainThePlaintext() {
        assertFalse(String(cipher.encrypt(secret), Charsets.ISO_8859_1).contains("test secret"))
    }

    @Test
    fun decrypt_changedCiphertext_fails() {
        val changed = cipher.encrypt(secret).also { it[it.lastIndex] = (it[it.lastIndex] + 1).toByte() }

        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(changed) }
    }
}
