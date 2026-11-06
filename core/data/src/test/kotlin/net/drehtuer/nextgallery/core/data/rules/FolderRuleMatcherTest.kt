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
    fun isIndexed_pathOutsideIncludes_returnsFalse() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM", INCLUDE)))

        assertFalse(matcher.isIndexed("Pictures/"))
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
    fun isIndexed_siblingWithSamePrefixAsExclude_returnsTrue() {
        val matcher = FolderRuleMatcher(listOf(FolderRule("DCIM/Cam", EXCLUDE)))

        assertTrue(matcher.isIndexed("DCIM/Camera/"))
    }
}
