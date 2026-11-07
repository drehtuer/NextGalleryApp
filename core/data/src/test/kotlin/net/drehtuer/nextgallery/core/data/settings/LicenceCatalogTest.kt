package net.drehtuer.nextgallery.core.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenceCatalogTest {
    private fun library(name: String) =
        """{"uniqueId":"group:$name","artifactVersion":"1.0","name":"$name","licenses":["Apache-2.0"]}"""

    @Test
    fun parse_readsAllFields() {
        val json = """
            {"libraries":[{"uniqueId":"a:b","artifactVersion":"1.2","name":"Lib","licenses":["MIT","Apache-2.0"]}]}
        """.trimIndent()

        assertEquals(
            listOf(LibraryLicence("a:b", "Lib", "1.2", listOf("MIT", "Apache-2.0"))),
            LicenceCatalog.parse(json),
        )
    }

    @Test
    fun parse_sortsByNameIgnoringCase() {
        val json = """{"libraries":[${library("zlib")},${library("Okio")},${library("activity")}]}"""

        assertEquals(listOf("activity", "Okio", "zlib"), LicenceCatalog.parse(json).map { it.name })
    }

    @Test
    fun parse_invalidJson_isEmpty() {
        assertTrue(LicenceCatalog.parse("not json").isEmpty())
    }

    @Test
    fun parse_noLibraries_isEmpty() {
        assertTrue(LicenceCatalog.parse("{}").isEmpty())
    }

    @Test
    fun parse_librariesOfTheWrongType_isEmpty() {
        assertTrue(LicenceCatalog.parse("""{"libraries":"none"}""").isEmpty())
    }
}
