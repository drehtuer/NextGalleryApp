package net.drehtuer.nextgallery.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun deleteIfPossible_existingFile_removesIt() {
        val file = folder.newFile()

        file.deleteIfPossible()

        assertFalse(file.exists())
    }

    @Test
    fun deleteIfPossible_missingFile_doesNotFail() {
        File(folder.root, "missing").deleteIfPossible()
    }

    @Test
    fun deleteIfPossible_folderWithContent_keepsItWithoutFailing() {
        val subfolder = folder.newFolder()
        File(subfolder, "file").createNewFile()

        subfolder.deleteIfPossible()

        assertTrue(subfolder.exists())
    }
}
