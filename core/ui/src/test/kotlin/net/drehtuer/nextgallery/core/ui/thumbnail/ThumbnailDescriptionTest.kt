package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class ThumbnailDescriptionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun thumbnailDescription_isNameAndLocalDate() {
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        var description = ""
        composeRule.setContent { description = thumbnailDescription(item(NOON_23_OCT_2026_UTC)) }

        composeRule.runOnIdle { assertEquals("IMG_1.jpg, 23 Oct 2026", description) }
        TimeZone.setDefault(previous)
    }

    private fun item(dateTakenMillis: Long) = MediaItem(
        id = MediaId(1),
        volumeName = "external_primary",
        contentUri = "content://media/1",
        bucketId = 1,
        bucketName = "Camera",
        relativePath = "DCIM/Camera/",
        displayName = "IMG_1.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 1,
        width = 100,
        height = 100,
        orientation = 0,
        dateTakenMillis = dateTakenMillis,
        dateSource = DateSource.DATE_TAKEN,
        takenLocalDate = LocalDate.of(2026, 10, 23),
        dateModifiedSeconds = 1,
        generationModified = 1,
        location = null,
    )

    private companion object {
        const val NOON_23_OCT_2026_UTC = 1_792_756_800_000L
    }
}
