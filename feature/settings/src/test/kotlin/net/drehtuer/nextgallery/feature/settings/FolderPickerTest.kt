package net.drehtuer.nextgallery.feature.settings

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderPickerTest {
    @Test
    fun treeDocumentIdOf_externalStorage_isTheDecodedId() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FCamera")

        assertEquals("primary:DCIM/Camera", treeDocumentIdOf(uri))
    }

    @Test
    fun treeDocumentIdOf_otherProvider_isEmpty() {
        val uri = Uri.parse("content://org.example.cloud.documents/tree/primary%3ADCIM")

        assertEquals("", treeDocumentIdOf(uri))
    }
}
