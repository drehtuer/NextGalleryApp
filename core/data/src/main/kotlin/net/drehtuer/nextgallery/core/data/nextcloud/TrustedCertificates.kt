package net.drehtuer.nextgallery.core.data.nextcloud

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.nextcloud.CertificatePinSource
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** The certificates the user accepted, with one per host (D32). */
interface TrustedCertificateStore {
    fun observe(): Flow<List<CertificateInfo>>

    suspend fun put(info: CertificateInfo)

    suspend fun remove(host: String)
}

/** A file that can't be read means no pins, so that the user is asked again rather than a certificate being trusted. */
class DataStoreTrustedCertificateStore(private val dataStore: DataStore<Preferences>) : TrustedCertificateStore {
    override fun observe(): Flow<List<CertificateInfo>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[CERTIFICATES].orEmpty().mapNotNull(::decode).sortedBy { it.host } }

    override suspend fun put(info: CertificateInfo) = edit { entries ->
        entries.filterNot { it.host == info.host } + info
    }

    override suspend fun remove(host: String) = edit { entries -> entries.filterNot { it.host == host } }

    private suspend fun edit(transform: (List<CertificateInfo>) -> List<CertificateInfo>) {
        dataStore.edit { preferences ->
            val entries = preferences[CERTIFICATES].orEmpty().mapNotNull(::decode)
            preferences[CERTIFICATES] = transform(entries).map(::encode).toSet()
        }
    }

    private fun encode(info: CertificateInfo): String = buildJsonObject {
        put(HOST, info.host)
        put(SUBJECT, info.subject)
        put(ISSUER, info.issuer)
        put(NOT_BEFORE, info.notBeforeMillis)
        put(NOT_AFTER, info.notAfterMillis)
        put(FINGERPRINT, info.sha256Fingerprint)
    }.toString()

    private fun decode(entry: String): CertificateInfo? = try {
        val json = Json.parseToJsonElement(entry).jsonObject
        CertificateInfo(
            host = json.getValue(HOST).jsonPrimitive.content,
            subject = json.getValue(SUBJECT).jsonPrimitive.content,
            issuer = json.getValue(ISSUER).jsonPrimitive.content,
            notBeforeMillis = json.getValue(NOT_BEFORE).jsonPrimitive.long,
            notAfterMillis = json.getValue(NOT_AFTER).jsonPrimitive.long,
            sha256Fingerprint = json.getValue(FINGERPRINT).jsonPrimitive.content,
        )
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: NoSuchElementException) {
        null
    }

    companion object {
        private val CERTIFICATES = stringSetPreferencesKey("certificates")
        private const val HOST = "host"
        private const val SUBJECT = "subject"
        private const val ISSUER = "issuer"
        private const val NOT_BEFORE = "notBefore"
        private const val NOT_AFTER = "notAfter"
        private const val FINGERPRINT = "fingerprint"

        fun create(file: File) = DataStoreTrustedCertificateStore(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { file },
            ),
        )
    }
}

/** Gives the TLS checks the pins. The handshake runs on its own thread, so blocking here is fine. */
class StoredCertificatePins @Inject constructor(private val store: TrustedCertificateStore) : CertificatePinSource {
    override fun pinnedFingerprint(host: String): String? =
        runBlocking { store.observe().first().firstOrNull { it.host == host }?.sha256Fingerprint }
}

interface TrustedCertificateRepository {
    fun observe(): Flow<List<CertificateInfo>>

    /** The user accepted [info] in the warning dialog. This is the only way a certificate gets pinned. */
    suspend fun accept(info: CertificateInfo): AppResult<Unit>

    suspend fun remove(host: String): AppResult<Unit>
}

class DefaultTrustedCertificateRepository @Inject constructor(
    private val store: TrustedCertificateStore,
    private val connections: ConnectionEvictor,
) : TrustedCertificateRepository {
    override fun observe(): Flow<List<CertificateInfo>> = store.observe()

    override suspend fun accept(info: CertificateInfo): AppResult<Unit> = runCatchingCancellable {
        store.put(info.copy(host = info.host.lowercase(), pinnedFingerprint = null))
        connections.evictConnections()
    }

    /** Open connections and resumable sessions were checked against the old pin, so they have to go as well. */
    override suspend fun remove(host: String): AppResult<Unit> = runCatchingCancellable {
        store.remove(host)
        connections.evictConnections()
    }
}
