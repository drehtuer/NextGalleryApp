package net.drehtuer.nextgallery.core.platform

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException

/**
 * Robolectric has no Android Keystore, which is the failure Tink falls back on a clear text keyset for. The working
 * Keystore is covered by the instrumented `KeystoreSecretCipherTest`.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSecretCipherFallbackTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val cipher = KeystoreSecretCipher(application)

    @Test
    fun encrypt_keystoreUnavailable_failsInsteadOfUsingAClearTextKeyset() {
        assertThrows(GeneralSecurityException::class.java) { cipher.encrypt("secret".toByteArray()) }
    }

    @Test
    fun encrypt_keystoreUnavailable_leavesNoKeysetBehind() {
        assertThrows(GeneralSecurityException::class.java) { cipher.encrypt("secret".toByteArray()) }

        val preferences = application.getSharedPreferences("nextgallery_keyset_preferences", Context.MODE_PRIVATE)
        assertFalse(preferences.contains("nextgallery_keyset"))
    }
}
