package net.drehtuer.nextgallery.core.data.nextcloud

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.nextcloud.CredentialsProvider
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Base64
import javax.inject.Inject

/** The one account of the app (D14). The file holds only ciphertext: nothing is readable without the key. */
interface AccountStore {
    fun observeAccount(): Flow<NextcloudAccount?>

    suspend fun save(account: NextcloudAccount)

    suspend fun clear()
}

class DefaultAccountStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SecretCipher,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AccountStore {
    override fun observeAccount(): Flow<NextcloudAccount?> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[ACCOUNT]?.let(::decode) }
        .distinctUntilChanged()
        .flowOn(io)

    override suspend fun save(account: NextcloudAccount) {
        val encrypted = withContext(io) { encode(account) }
        dataStore.edit { it[ACCOUNT] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun encode(account: NextcloudAccount): String {
        val json = buildJsonObject {
            put(SERVER_URL, account.serverUrl)
            put(LOGIN_NAME, account.loginName)
            put(USER_ID, account.userId)
            put(APP_PASSWORD, account.appPassword)
        }.toString()
        return Base64.getEncoder().encodeToString(cipher.encrypt(json.toByteArray()))
    }

    /** Data that can't be decrypted, e.g. after the Keystore key was lost, reads as "not signed in". */
    private fun decode(stored: String): NextcloudAccount? = try {
        val json = Json.parseToJsonElement(String(cipher.decrypt(Base64.getDecoder().decode(stored)))).jsonObject
        NextcloudAccount(
            serverUrl = json.getValue(SERVER_URL).jsonPrimitive.content,
            loginName = json.getValue(LOGIN_NAME).jsonPrimitive.content,
            userId = json.getValue(USER_ID).jsonPrimitive.content,
            appPassword = json.getValue(APP_PASSWORD).jsonPrimitive.content,
        )
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: NoSuchElementException) {
        null
    }

    companion object {
        private val ACCOUNT = stringPreferencesKey("account")
        private const val SERVER_URL = "serverUrl"
        private const val LOGIN_NAME = "loginName"
        private const val USER_ID = "userId"
        private const val APP_PASSWORD = "appPassword"

        /** A corrupt file reads as "not signed in", as if the data could not be decrypted. */
        fun create(file: File, cipher: SecretCipher) = DefaultAccountStore(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { file },
            ),
            cipher,
        )
    }
}

/** Gives the HTTP clients the current account. They ask from their own threads, so blocking here is fine. */
class StoredCredentialsProvider @Inject constructor(private val store: AccountStore) : CredentialsProvider {
    override fun current(): NextcloudAccount? = runBlocking { store.observeAccount().first() }
}
