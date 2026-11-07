package net.drehtuer.nextgallery.core.data.upload

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeWorkScheduler
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.nextcloud.FakeAccountStore
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.NEXTCLOUD_APP_PACKAGE
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadHandoff
import net.drehtuer.nextgallery.core.model.UploadState
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultUploadHandoffRepositoryTest {
    private val accounts = FakeAccountStore()
    private val indexStore = FakeMediaIndexStore()
    private val stateStore = FakeUploadStateStore()
    private val scheduler = FakeWorkScheduler()
    private var installed = true
    private val repository = DefaultUploadHandoffRepository(
        accounts = accounts,
        appSource = { installed },
        indexStore = indexStore,
        stateStore = stateStore,
        workScheduler = scheduler,
    )
    private val ids = listOf(MediaId(1), MediaId(2))

    private suspend fun indexed() = indexStore.upsertAll(listOf(mediaItem(1), mediaItem(2)))

    @Test
    fun prepare_nothingUploaded_targetsTheNextcloudApp() = runTest {
        indexed()

        val expected = ShareSpec(listOf("content://media/1", "content://media/2"), "image/jpeg", NEXTCLOUD_APP_PACKAGE)
        assertEquals(UploadHandoff.Ready(expected), repository.prepare(ids))
    }

    @Test
    fun prepare_dropsWhatIsAlreadyUploaded() = runTest {
        indexed()
        stateStore.states.value = mapOf(MediaId(1) to UploadState.Uploaded(verifiedByChecksum = false))

        val handoff = repository.prepare(ids) as UploadHandoff.Ready

        assertEquals(listOf("content://media/2"), handoff.spec.contentUris)
    }

    @Test
    fun prepare_keepsWhatIsNotKnownToBeUploaded() = runTest {
        indexed()
        stateStore.states.value = mapOf(MediaId(1) to UploadState.NotUploaded, MediaId(2) to UploadState.Unknown)

        val handoff = repository.prepare(ids) as UploadHandoff.Ready

        assertEquals(2, handoff.spec.contentUris.size)
    }

    @Test
    fun prepare_allUploaded_hasNothingToUpload() = runTest {
        indexed()
        stateStore.states.value = ids.associateWith { UploadState.Uploaded(verifiedByChecksum = true) }

        assertEquals(UploadHandoff.NothingToUpload, repository.prepare(ids))
    }

    @Test
    fun prepare_appNotInstalled_saysSo() = runTest {
        indexed()
        installed = false

        assertEquals(UploadHandoff.NextcloudAppMissing, repository.prepare(ids))
    }

    @Test
    fun onAppResumed_afterHandoff_requestsACheck() = runTest {
        indexed()
        repository.prepare(ids)

        repository.onAppResumed()

        assertEquals(1, scheduler.uploadStateRequests)
    }

    @Test
    fun onAppResumed_secondTime_requestsNoFurtherCheck() = runTest {
        indexed()
        repository.prepare(ids)
        repository.onAppResumed()

        repository.onAppResumed()

        assertEquals(1, scheduler.uploadStateRequests)
    }

    @Test
    fun onAppResumed_withoutHandoff_requestsNoCheck() {
        repository.onAppResumed()

        assertEquals(0, scheduler.uploadStateRequests)
    }

    @Test
    fun onAppResumed_afterNothingToUpload_requestsNoCheck() = runTest {
        repository.prepare(emptyList())

        repository.onAppResumed()

        assertEquals(0, scheduler.uploadStateRequests)
    }

    @Test
    fun observeAvailability_loggedOut_isHidden() = runTest {
        accounts.account.value = null

        repository.observeAvailability().test {
            assertEquals(UploadAvailability.HIDDEN, awaitItem())
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun observeAvailability_loggedInWithApp_isAvailable() = runTest {
        repository.observeAvailability().test {
            skipItems(1)
            assertEquals(UploadAvailability.AVAILABLE, awaitItem())
            cancel()
        }
    }

    @Test
    fun observeAvailability_loggedInWithoutApp_isAppMissing() = runTest {
        installed = false

        repository.observeAvailability().test {
            skipItems(1)
            assertEquals(UploadAvailability.APP_MISSING, awaitItem())
            cancel()
        }
    }

    @Test
    fun observeAvailability_appInstalledMeanwhile_isSeenOnResume() = runTest {
        installed = false

        repository.observeAvailability().test {
            skipItems(2)
            installed = true
            repository.onAppResumed()
            assertEquals(UploadAvailability.AVAILABLE, awaitItem())
            cancel()
        }
    }

    @Test
    fun observeAvailability_logout_hidesTheAction() = runTest {
        repository.observeAvailability().test {
            skipItems(2)
            accounts.account.value = null
            assertEquals(UploadAvailability.HIDDEN, awaitItem())
            cancel()
        }
    }
}
