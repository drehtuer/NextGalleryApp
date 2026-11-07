package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class TrustedCertificatesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val info = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")
    private val file by lazy { File(folder.root, "trusted.preferences_pb") }
    private val store by lazy { DataStoreTrustedCertificateStore.create(file) }

    private val mockStore = mockk<TrustedCertificateStore>(relaxed = true)
    private val connections = mockk<ConnectionEvictor>(relaxed = true)
    private val repository = DefaultTrustedCertificateRepository(mockStore, connections)

    @Test
    fun store_put_isReadBackWithAllFields() = runTest {
        store.put(info)

        assertEquals(listOf(info), store.observe().first())
    }

    @Test
    fun store_putForSameHost_replacesTheOldCertificate() = runTest {
        store.put(info)

        store.put(info.copy(sha256Fingerprint = "CC:DD"))

        assertEquals(listOf("CC:DD"), store.observe().first().map { it.sha256Fingerprint })
    }

    @Test
    fun store_remove_removesOnlyThatHost() = runTest {
        store.put(info)
        store.put(info.copy(host = "other.example.com"))

        store.remove("cloud.example.com")

        assertEquals(listOf("other.example.com"), store.observe().first().map { it.host })
    }

    @Test
    fun store_corruptFile_hasNoPins() = runTest {
        file.writeText("this is not a preferences file")

        assertTrue(store.observe().first().isEmpty())
    }

    @Test
    fun pins_knownHost_returnsItsFingerprint() = runTest {
        store.put(info)

        assertEquals("AA:BB", StoredCertificatePins(store).pinnedFingerprint("cloud.example.com"))
    }

    @Test
    fun pins_unknownHost_isNull() = runTest {
        store.put(info)

        assertNull(StoredCertificatePins(store).pinnedFingerprint("other.example.com"))
    }

    @Test
    fun repository_accept_storesWithoutTheOldFingerprintAndLowerCaseHost() = runTest {
        repository.accept(info.copy(host = "Cloud.Example.com", pinnedFingerprint = "OLD"))

        coVerify { mockStore.put(info) }
    }

    @Test
    fun repository_accept_evictsConnections() = runTest {
        repository.accept(info)

        verify { connections.evictConnections() }
    }

    @Test
    fun repository_remove_removesAndEvictsConnections() = runTest {
        repository.remove("cloud.example.com")

        coVerify { mockStore.remove("cloud.example.com") }
        verify { connections.evictConnections() }
    }

    @Test
    fun repository_storeFails_returnsFailure() = runTest {
        coEvery { mockStore.put(any()) } throws IOException("disk full")

        assertEquals(AppResult.Failure(AppError.Io("disk full")), repository.accept(info))
    }

    @Test
    fun repository_observe_comesFromTheStore() = runTest {
        io.mockk.every { mockStore.observe() } returns flowOf(listOf(info))

        assertEquals(listOf(info), repository.observe().first())
    }
}
