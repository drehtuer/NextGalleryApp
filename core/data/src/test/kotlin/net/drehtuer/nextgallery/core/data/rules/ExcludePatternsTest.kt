package net.drehtuer.nextgallery.core.data.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcludePatternsTest {
    @Test
    fun parse_splitsLinesAndDropsBlanksAndDuplicates() {
        assertEquals(listOf("a*", "b"), ExcludePatterns.parse(" a* \n\n b\na*\n"))
    }

    @Test
    fun toRegex_star_matchesAnyCharactersAcrossFolders() {
        assertTrue(ExcludePatterns.toRegex("*Thumbs").matches("Pictures/Old/Thumbs"))
    }

    @Test
    fun toRegex_noStar_matchesOnlyThatPath() {
        assertFalse(ExcludePatterns.toRegex("Thumbs").matches("Pictures/Thumbs"))
    }

    @Test
    fun toRegex_matchedFolder_alsoCoversSubfolders() {
        assertTrue(ExcludePatterns.toRegex("Pictures/T*").matches("Pictures/Thumbs/Small"))
    }

    @Test
    fun toRegex_samePrefixSibling_doesNotMatch() {
        assertFalse(ExcludePatterns.toRegex("Pictures/Thumbs").matches("Pictures/Thumbs2"))
    }

    @Test
    fun toRegex_differentCase_matches() {
        assertTrue(ExcludePatterns.toRegex("pictures/*").matches("Pictures/Holiday"))
    }

    @Test
    fun toRegex_trailingSlash_isIgnored() {
        assertTrue(ExcludePatterns.toRegex("Pictures/").matches("Pictures"))
    }

    @Test
    fun toRegex_regexCharacters_areLiteral() {
        assertFalse(ExcludePatterns.toRegex("a.c").matches("abc"))
    }
}
