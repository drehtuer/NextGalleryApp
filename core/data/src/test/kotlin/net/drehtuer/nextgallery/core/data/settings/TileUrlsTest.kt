package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileUrlsTest {
    @Test
    fun isValid_defaultUrl_isTrue() {
        assertTrue(TileUrls.isValid(UserSettings.DEFAULT_TILE_URL))
    }

    @Test
    fun isValid_http_isFalse() {
        assertFalse(TileUrls.isValid("http://tiles.example.org/{z}/{x}/{y}.png"))
    }

    @Test
    fun isValid_missingPlaceholder_isFalse() {
        assertFalse(TileUrls.isValid("https://tiles.example.org/{z}/{x}.png"))
    }

    @Test
    fun isValid_whitespace_isFalse() {
        assertFalse(TileUrls.isValid("https://tiles example.org/{z}/{x}/{y}.png"))
    }

    @Test
    fun isValid_empty_isFalse() {
        assertFalse(TileUrls.isValid(""))
    }
}
