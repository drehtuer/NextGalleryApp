package net.drehtuer.nextgallery.core.data.media

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultMediaRepositoryTest {
    private val store = FakeMediaIndexStore()

    @Test
    fun observeIndexedCount_returnsStoreCount() = runTest {
        assertEquals(0, DefaultMediaRepository(store).observeIndexedCount().first())
    }

    @Test
    fun getItem_unknownId_returnsNull() = runTest {
        assertNull(DefaultMediaRepository(store).getItem(MediaId(1)))
    }
}
