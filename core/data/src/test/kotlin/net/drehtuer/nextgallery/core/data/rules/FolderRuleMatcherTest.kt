package net.drehtuer.nextgallery.core.data.rules

import net.drehtuer.nextgallery.core.model.FolderRule
import net.drehtuer.nextgallery.core.model.FolderRule.Type.EXCLUDE
import net.drehtuer.nextgallery.core.model.FolderRule.Type.INCLUDE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderRuleMatcherTest {
    @Test
    fun isIndexed_noRules_returnsTrue() {
        assertTrue(FolderRuleMatcher(emptyList()).isIndexed("Pictures/"))
    }

    @Test
    fun isIndexed_pathUnderInclude_returnsTrue() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM", INCLUDE)))

        assertTrue(matcher.isIndexed("DCIM/Camera/"))
    }

    @Test
    fun isIndexed_pathOutsideIncludes_returnsTrue() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM", INCLUDE)))

        assertTrue(matcher.isIndexed("Pictures/"))
    }

    @Test
    fun isIndexed_pathUnderExclude_returnsFalse() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("Pictures/Screenshots", EXCLUDE)))

        assertFalse(matcher.isIndexed("Pictures/Screenshots/"))
    }

    @Test
    fun isIndexed_excludeInsideInclude_excludeWins() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM", INCLUDE), FolderRule("DCIM/Camera", EXCLUDE)))

        assertFalse(matcher.isIndexed("DCIM/Camera/"))
    }

    @Test
    fun isIndexed_includeInsideExclude_includeWins() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("Android", EXCLUDE), FolderRule("Android/media/x", INCLUDE)))

        assertTrue(matcher.isIndexed("Android/media/x/"))
    }

    @Test
    fun isIndexed_samePathInBothLists_excludeWins() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM", INCLUDE), FolderRule("DCIM", EXCLUDE)))

        assertFalse(matcher.isIndexed("DCIM/"))
    }

    @Test
    fun isIndexed_pathMatchingPattern_returnsFalse() {
        val matcher = FolderRuleMatcher(emptyList(), listOf("*/Thumbs"))

        assertFalse(matcher.isIndexed("Pictures/Thumbs/"))
    }

    @Test
    fun isIndexed_pathNotMatchingPattern_returnsTrue() {
        val matcher = FolderRuleMatcher(emptyList(), listOf("*/Thumbs"))

        assertTrue(matcher.isIndexed("Pictures/Holiday/"))
    }

    @Test
    fun isIndexed_includedFolderMatchingPattern_includeWins() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("Pictures/Thumbs", INCLUDE)), listOf("*/Thumbs"))

        assertTrue(matcher.isIndexed("Pictures/Thumbs/"))
    }

    @Test
    fun isIndexed_siblingWithSamePrefixAsExclude_returnsTrue() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM/Cam", EXCLUDE)))

        assertTrue(matcher.isIndexed("DCIM/Camera/"))
    }
}
