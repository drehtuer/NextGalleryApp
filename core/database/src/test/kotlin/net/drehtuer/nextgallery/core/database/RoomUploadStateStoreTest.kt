package net.drehtuer.nextgallery.core.database

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.upload.UploadStateRecord
import net.drehtuer.nextgallery.core.database.RoomMediaIndexStore.Companion.SQLITE_MAX_VARIABLES
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.UploadState
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomUploadStateStoreTest {
    private val dao = mockk<UploadStateDao>(relaxed = true)
    private val store = RoomUploadStateStore(dao)

    private fun record(id: Long, state: UploadState) = UploadStateRecord(
        mediaId = MediaId(id),
        remotePath = "/P/$id.jpg",
        state = state,
        localSize = 10,
        localDateModifiedSeconds = 20,
        remoteEtag = "file",
        remoteFolderEtag = "folder",
        checkedAtMillis = 30,
    )

    @Test
    fun observeStates_noIds_isEmptyWithoutAQuery() = runTest {
        assertEquals(emptyMap<MediaId, UploadState>(), store.observeStates(emptyList()).first())
    }

    @Test
    fun observeStates_mapsTheRows() = runTest {
        every { dao.observeStatusFor(listOf(1, 2)) } returns flowOf(
            listOf(
                UploadStatusRow(1, UploadStateEntity.STATE_NOT_UPLOADED, false),
                UploadStatusRow(2, UploadStateEntity.STATE_UPLOADED, true),
            ),
        )

        val states = store.observeStates(listOf(MediaId(1), MediaId(2))).first()

        val expected = mapOf(
            MediaId(1) to UploadState.NotUploaded,
            MediaId(2) to UploadState.Uploaded(verifiedByChecksum = true),
        )
        assertEquals(expected, states)
    }

    @Test
    fun observeStates_manyIds_combinesSeveralQueries() = runTest {
        every { dao.observeStatusFor(any()) } answers {
            flowOf(listOf(UploadStatusRow(firstArg<List<Long>>().first(), UploadStateEntity.STATE_NOT_UPLOADED, false)))
        }

        val states = store.observeStates(List(SQLITE_MAX_VARIABLES + 1) { MediaId(it.toLong()) }).first()

        assertEquals(setOf(MediaId(0), MediaId(SQLITE_MAX_VARIABLES.toLong())), states.keys)
    }

    @Test
    fun states_unknownStateValue_isUnknown() = runTest {
        coEvery { dao.statusFor(listOf(1)) } returns listOf(UploadStatusRow(1, UploadStateEntity.STATE_UNKNOWN, false))

        assertEquals(mapOf(MediaId(1) to UploadState.Unknown), store.states(listOf(MediaId(1))))
    }

    @Test
    fun localFolders_comesFromTheDao() = runTest {
        coEvery { dao.localFolders() } returns listOf("DCIM/")

        assertEquals(listOf("DCIM/"), store.localFolders())
    }

    @Test
    fun itemsInFolder_mapsTheRows() = runTest {
        coEvery { dao.mediaInFolder("DCIM/") } returns listOf(mediaEntity(1))

        assertEquals(listOf(MediaId(1)), store.itemsInFolder("DCIM/").map { it.id })
    }

    @Test
    fun record_survivesTheWayThroughTheDatabase_uploaded() = runTest {
        val original = record(1, UploadState.Uploaded(verifiedByChecksum = true))
        coEvery { dao.rowsInFolder("DCIM/") } returns listOf(original.toEntity())

        assertEquals(listOf(original), store.recordsInFolder("DCIM/"))
    }

    @Test
    fun record_survivesTheWayThroughTheDatabase_notUploaded() = runTest {
        val original = record(1, UploadState.NotUploaded)
        coEvery { dao.rowsInFolder("DCIM/") } returns listOf(original.toEntity())

        assertEquals(listOf(original), store.recordsInFolder("DCIM/"))
    }

    @Test
    fun record_survivesTheWayThroughTheDatabase_unknown() = runTest {
        val original = record(1, UploadState.Unknown)
        coEvery { dao.rowsInFolder("DCIM/") } returns listOf(original.toEntity())

        assertEquals(listOf(original), store.recordsInFolder("DCIM/"))
    }

    @Test
    fun upsertAll_manyRecords_writesInBatches() = runTest {
        store.upsertAll(List(SQLITE_MAX_VARIABLES + 1) { record(it.toLong(), UploadState.NotUploaded) })

        coVerify(exactly = 2) { dao.upsertExisting(any()) }
    }

    @Test
    fun clear_clearsTheTable() = runTest {
        store.clear()

        coVerify { dao.clear() }
    }
}
