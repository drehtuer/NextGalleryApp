package net.drehtuer.nextgallery.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativePathsTest {
    @Test
    fun normalize_removesLeadingTrailingAndDuplicateSlashes() {
        assertEquals("DCIM/Camera", RelativePaths.normalize("/DCIM//Camera/"))
    }

    @Test
    fun isUnder_samePath_returnsTrue() {
        assertTrue(RelativePaths.isUnder("DCIM/Camera/", "DCIM/Camera"))
    }

    @Test
    fun isUnder_subfolder_returnsTrue() {
        assertTrue(RelativePaths.isUnder("DCIM/Camera/2024/", "DCIM"))
    }

    @Test
    fun isUnder_siblingWithSamePrefix_returnsFalse() {
        assertFalse(RelativePaths.isUnder("DCIM/Camera/", "DCIM/Cam"))
    }

    @Test
    fun isUnder_differentCase_returnsTrue() {
        assertTrue(RelativePaths.isUnder("dcim/camera/", "DCIM/Camera"))
    }

    @Test
    fun isUnder_emptyParent_returnsTrue() {
        assertTrue(RelativePaths.isUnder("Pictures/", ""))
    }
}
