package net.drehtuer.nextgallery.feature.editor

import net.drehtuer.nextgallery.core.model.edit.ImageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleInputTest {
    private val original = ImageSize(4000, 3000)
    private val initial = ScaleInput.initial(original)

    @Test
    fun initial_showsOriginalSizeAtFullPercent() {
        assertEquals(ScaleInput(original, "100", "4000", "3000"), initial)
    }

    @Test
    fun initial_cannotBeApplied() {
        assertFalse(initial.canApply)
    }

    @Test
    fun withPercent_updatesWidthAndHeight() {
        assertEquals(ScaleInput(original, "50", "2000", "1500"), initial.withPercent("50"))
    }

    @Test
    fun withWidth_updatesPercentAndHeight() {
        assertEquals(ScaleInput(original, "50", "2000", "1500"), initial.withWidth("2000"))
    }

    @Test
    fun withHeight_updatesPercentAndWidth() {
        assertEquals(ScaleInput(original, "50", "2000", "1500"), initial.withHeight("1500"))
    }

    @Test
    fun withPercent_empty_clearsTheOthers() {
        assertEquals(ScaleInput(original, "", "", ""), initial.withPercent(""))
    }

    @Test
    fun withWidth_empty_clearsTheOthers() {
        assertEquals(ScaleInput(original, "", "", ""), initial.withWidth(""))
    }

    @Test
    fun withHeight_empty_clearsTheOthers() {
        assertEquals(ScaleInput(original, "", "", ""), initial.withHeight(""))
    }

    @Test
    fun withPercent_otherCharacters_areDropped() {
        assertEquals("50", initial.withPercent("5a0-").percent)
    }

    @Test
    fun withWidth_tooManyDigits_areCut() {
        assertEquals("123456", initial.withWidth("1234567890").width)
    }

    @Test
    fun target_validFields_isTheNewSize() {
        assertEquals(ImageSize(2000, 1500), initial.withPercent("50").target)
    }

    @Test
    fun target_zero_isNull() {
        assertNull(initial.withWidth("0").target)
    }

    @Test
    fun target_aboveTheLimit_isNull() {
        assertNull(initial.withWidth("40000").target)
    }

    @Test
    fun canApply_newSize_isTrue() {
        assertTrue(initial.withPercent("50").canApply)
    }

    @Test
    fun canApply_noTarget_isFalse() {
        assertFalse(initial.withPercent("").canApply)
    }
}
