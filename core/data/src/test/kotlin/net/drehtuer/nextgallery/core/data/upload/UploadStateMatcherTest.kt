package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.UploadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadStateMatcherTest {
    private val item = mediaItem(1).copy(displayName = "a.jpg", relativePath = "DCIM/", sizeBytes = 100)
    private val remotePath = "/P/DCIM/a.jpg"
    private val digests = mutableListOf<Pair<MediaItem, ChecksumAlgorithm>>()

    private fun remote(size: Long = 100, checksums: String? = null, name: String = "a.jpg", etag: String? = "file-1") =
        RemoteItem(remotePath, name, isDirectory = false, size = size, etag = etag, checksums = checksums)

    private fun record(
        state: UploadState,
        etag: String? = "file-1",
        path: String = remotePath,
        dateModified: Long = 1,
    ) = UploadStateRecord(item.id, path, state, 100, dateModified, etag, "folder-old", 1)

    private suspend fun match(
        remoteFiles: List<RemoteItem>,
        previous: UploadStateRecord? = null,
        localDigest: String? = "aa",
    ): UploadStateRecord {
        val comparison = FolderComparison("/P", "folder-1", listOfNotNull(previous).associateBy { it.mediaId }, 500)
        return UploadStateMatcher.match(listOf(item), remoteFiles, comparison) { media, algorithm ->
            digests += media to algorithm
            localDigest
        }.single()
    }

    @Test
    fun match_sameNameAndSizeWithoutChecksum_isUploadedUnconfirmed() = runTest {
        assertEquals(UploadState.Uploaded(verifiedByChecksum = false), match(listOf(remote())).state)
    }

    @Test
    fun match_noChecksum_doesNotReadTheImage() = runTest {
        match(listOf(remote()))

        assertEquals(emptyList<Any>(), digests)
    }

    @Test
    fun match_sizeDiffers_isNotUploaded() = runTest {
        assertEquals(UploadState.NotUploaded, match(listOf(remote(size = 99))).state)
    }

    @Test
    fun match_sizeDiffers_doesNotReadTheImage() = runTest {
        match(listOf(remote(size = 99, checksums = "SHA1:aa")))

        assertEquals(emptyList<Any>(), digests)
    }

    @Test
    fun match_notOnTheServer_isNotUploaded() = runTest {
        assertEquals(UploadState.NotUploaded, match(emptyList()).state)
    }

    @Test
    fun match_notOnTheServer_hasNoRemoteEtag() = runTest {
        assertNull(match(emptyList()).remoteEtag)
    }

    @Test
    fun match_folderWithTheSameName_isNotUploaded() = runTest {
        val folder = RemoteItem(remotePath, "a.jpg", isDirectory = true, size = 100)

        assertEquals(UploadState.NotUploaded, match(listOf(folder)).state)
    }

    @Test
    fun match_otherNameOfTheSameSize_isNotUploaded() = runTest {
        assertEquals(UploadState.NotUploaded, match(listOf(remote(name = "a (2).jpg"))).state)
    }

    @Test
    fun match_checksumMatches_isUploadedConfirmed() = runTest {
        assertEquals(
            UploadState.Uploaded(verifiedByChecksum = true),
            match(listOf(remote(checksums = "SHA1:AA"))).state,
        )
    }

    @Test
    fun match_checksumDiffers_isNotUploaded() = runTest {
        assertEquals(UploadState.NotUploaded, match(listOf(remote(checksums = "SHA1:bb"))).state)
    }

    @Test
    fun match_bothChecksums_usesSha1() = runTest {
        match(listOf(remote(checksums = "MD5:cc SHA1:aa")))

        assertEquals(listOf(item to ChecksumAlgorithm.SHA1), digests)
    }

    @Test
    fun match_imageCannotBeRead_isUploadedUnconfirmed() = runTest {
        val state = match(listOf(remote(checksums = "SHA1:aa")), localDigest = null).state

        assertEquals(UploadState.Uploaded(verifiedByChecksum = false), state)
    }

    @Test
    fun match_confirmedBeforeForTheSameFiles_doesNotReadTheImageAgain() = runTest {
        val previous = record(UploadState.Uploaded(verifiedByChecksum = true))

        val state = match(listOf(remote(checksums = "SHA1:aa")), previous, localDigest = "other").state

        assertEquals(UploadState.Uploaded(verifiedByChecksum = true), state)
        assertEquals(emptyList<Any>(), digests)
    }

    @Test
    fun match_mismatchBeforeForTheSameFiles_doesNotReadTheImageAgain() = runTest {
        val state = match(listOf(remote(checksums = "SHA1:aa")), record(UploadState.NotUploaded)).state

        assertEquals(UploadState.NotUploaded, state)
        assertEquals(emptyList<Any>(), digests)
    }

    @Test
    fun match_unconfirmedBefore_comparesNow() = runTest {
        val previous = record(UploadState.Uploaded(verifiedByChecksum = false))

        val state = match(listOf(remote(checksums = "SHA1:aa")), previous).state

        assertEquals(UploadState.Uploaded(verifiedByChecksum = true), state)
    }

    @Test
    fun match_serverFileChanged_comparesAgain() = runTest {
        val previous = record(UploadState.Uploaded(verifiedByChecksum = true), etag = "file-old")

        match(listOf(remote(checksums = "SHA1:aa")), previous)

        assertEquals(1, digests.size)
    }

    @Test
    fun match_localFileChanged_comparesAgain() = runTest {
        val previous = record(UploadState.Uploaded(verifiedByChecksum = true), dateModified = 0)

        match(listOf(remote(checksums = "SHA1:aa")), previous)

        assertEquals(1, digests.size)
    }

    @Test
    fun match_otherRemotePath_comparesAgain() = runTest {
        val previous = record(UploadState.Uploaded(verifiedByChecksum = true), path = "/Old/DCIM/a.jpg")

        match(listOf(remote(checksums = "SHA1:aa")), previous)

        assertEquals(1, digests.size)
    }

    @Test
    fun match_serverWithoutEtags_comparesAgain() = runTest {
        val previous = record(UploadState.Uploaded(verifiedByChecksum = true), etag = null)

        match(listOf(remote(checksums = "SHA1:aa", etag = null)), previous)

        assertEquals(1, digests.size)
    }

    @Test
    fun match_recordHoldsWhatWasCompared() = runTest {
        val expected = UploadStateRecord(
            mediaId = item.id,
            remotePath = remotePath,
            state = UploadState.Uploaded(verifiedByChecksum = false),
            localSize = 100,
            localDateModifiedSeconds = 1,
            remoteEtag = "file-1",
            remoteFolderEtag = "folder-1",
            checkedAtMillis = 500,
        )

        assertEquals(expected, match(listOf(remote())))
    }
}
