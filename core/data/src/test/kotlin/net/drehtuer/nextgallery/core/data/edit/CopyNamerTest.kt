package net.drehtuer.nextgallery.core.data.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class CopyNamerTest {
    @Test
    fun nameFor_noCollision_addsSuffix() {
        assertEquals("IMG_1234_edited.jpg", CopyNamer.nameFor("IMG_1234.jpg", emptySet()))
    }

    @Test
    fun nameFor_collision_addsNumber() {
        assertEquals("IMG_1234_edited_2.jpg", CopyNamer.nameFor("IMG_1234.jpg", setOf("IMG_1234_edited.jpg")))
    }

    @Test
    fun nameFor_twoCollisions_countsOn() {
        val existing = setOf("IMG_1234_edited.jpg", "IMG_1234_edited_2.jpg")

        assertEquals("IMG_1234_edited_3.jpg", CopyNamer.nameFor("IMG_1234.jpg", existing))
    }

    @Test
    fun nameFor_collisionInOtherCase_counts() {
        assertEquals("a_edited_2.png", CopyNamer.nameFor("a.png", setOf("A_EDITED.PNG")))
    }

    @Test
    fun nameFor_editedCopy_doesNotStackSuffixes() {
        assertEquals("a_edited.jpg", CopyNamer.nameFor("a_edited_2.jpg", emptySet()))
    }

    @Test
    fun nameFor_noExtension_addsSuffix() {
        assertEquals("photo_edited", CopyNamer.nameFor("photo", emptySet()))
    }

    @Test
    fun nameFor_hiddenFileStyleName_keepsDotInStem() {
        assertEquals(".jpg_edited", CopyNamer.nameFor(".jpg", emptySet()))
    }

    @Test
    fun nameFor_severalDots_changesOnlyLastPart() {
        assertEquals("a.b_edited.jpg", CopyNamer.nameFor("a.b.jpg", emptySet()))
    }
}
