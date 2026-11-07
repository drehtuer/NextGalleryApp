package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.core.content.edit
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.nextcloud.SecretCipher
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM through Tink, with the keyset encrypted by a key in the Android Keystore (D11). The keyset stays on the
 * device: app data is never backed up (D42). If the Keystore fails while the keyset is created, Tink carries on with
 * the keyset in clear text and only logs a warning. That is refused here and the keyset removed, because at the next
 * start Tink would read it as it is, with the Keystore working again and [AndroidKeysetManager.isUsingKeystore] true.
 */
@Singleton
class KeystoreSecretCipher @Inject constructor(@param:ApplicationContext private val context: Context) : SecretCipher {
    private val aead: Aead by lazy {
        AeadConfig.register()
        val manager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFERENCES)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
        if (!manager.isUsingKeystore) {
            context.getSharedPreferences(KEYSET_PREFERENCES, Context.MODE_PRIVATE).edit(commit = true) {
                remove(KEYSET_NAME)
            }
            throw GeneralSecurityException("The Android Keystore is not available")
        }
        manager.keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun encrypt(plaintext: ByteArray): ByteArray = aead.encrypt(plaintext, ASSOCIATED_DATA)

    override fun decrypt(ciphertext: ByteArray): ByteArray = aead.decrypt(ciphertext, ASSOCIATED_DATA)

    private companion object {
        const val KEYSET_NAME = "nextgallery_keyset"
        const val KEYSET_PREFERENCES = "nextgallery_keyset_preferences"
        const val MASTER_KEY_URI = "android-keystore://nextgallery_master"
        val ASSOCIATED_DATA = "nextgallery-account".toByteArray()
    }
}
