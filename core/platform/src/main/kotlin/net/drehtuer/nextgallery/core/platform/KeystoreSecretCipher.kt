package net.drehtuer.nextgallery.core.platform

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.nextcloud.SecretCipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM through Tink, with the keyset encrypted by a key in the Android Keystore (D11). The keyset stays on the
 * device: app data is never backed up (D42).
 */
@Singleton
class KeystoreSecretCipher @Inject constructor(@param:ApplicationContext private val context: Context) : SecretCipher {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFERENCES)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
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
