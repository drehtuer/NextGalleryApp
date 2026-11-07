package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.RemoteItem
import java.io.File

/** Encryption for the stored account. `:core:platform` implements it with Tink and the Android Keystore (D11). */
interface SecretCipher {
    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(ciphertext: ByteArray): ByteArray
}

/** The last listing per remote folder, for offline browsing (`remote_cache`). */
interface RemoteCacheStore {
    suspend fun get(parentPath: String): List<RemoteItem>

    suspend fun replaceFolder(parentPath: String, items: List<RemoteItem>)

    suspend fun clear()
}

interface ConnectivitySource {
    val isOnline: Flow<Boolean>
}

/** What the UI requests at the start of a login (D39). */
const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

/** Whether the app may use the local network on Android 17 (D39). */
interface LocalNetworkPermissionSource {
    fun isGranted(): Boolean
}

/** The disk and memory cache of remote images. */
interface RemoteImageCache {
    suspend fun clear()
}

/** The temporary folder server images are downloaded to before sharing (D48). */
interface RemoteShareCache {
    fun clear()

    /** A new, empty place for a file called [name]; [index] keeps equal names apart. */
    fun newFile(index: Int, name: String): File

    fun contentUri(file: File): String
}
