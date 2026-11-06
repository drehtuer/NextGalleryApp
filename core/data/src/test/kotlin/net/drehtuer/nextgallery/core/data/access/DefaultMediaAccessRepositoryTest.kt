package net.drehtuer.nextgallery.core.data.access

import net.drehtuer.nextgallery.core.data.access.DefaultMediaAccessRepository.Companion.READ_MEDIA_IMAGES
import net.drehtuer.nextgallery.core.data.access.DefaultMediaAccessRepository.Companion.READ_MEDIA_VISUAL_USER_SELECTED
import net.drehtuer.nextgallery.core.model.MediaAccess
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultMediaAccessRepositoryTest {
    @Test
    fun currentAccess_readMediaImagesGranted_returnsFull() {
        val repository = DefaultMediaAccessRepository { setOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED) }

        assertEquals(MediaAccess.FULL, repository.currentAccess())
    }

    @Test
    fun currentAccess_onlyUserSelectedGranted_returnsPartial() {
        val repository = DefaultMediaAccessRepository { setOf(READ_MEDIA_VISUAL_USER_SELECTED) }

        assertEquals(MediaAccess.PARTIAL, repository.currentAccess())
    }

    @Test
    fun currentAccess_nothingGranted_returnsDenied() {
        val repository = DefaultMediaAccessRepository { emptySet() }

        assertEquals(MediaAccess.DENIED, repository.currentAccess())
    }

    @Test
    fun requestedPermissions_containsFullAndPartialPermission() {
        val repository = DefaultMediaAccessRepository { emptySet() }

        assertEquals(listOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED), repository.requestedPermissions)
    }
}
