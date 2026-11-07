package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceFileParserTest {
    @Test
    fun parse_validLine_readsAllColumns() {
        val places = PlaceFileParser.parse(sequenceOf("Testtown\tIT\t45.5\t10.5\t20000"))

        assertEquals(listOf(Place("Testtown", "IT", 45.5, 10.5, 20_000)), places)
    }

    @Test
    fun parse_comment_isSkipped() {
        assertTrue(PlaceFileParser.parse(sequenceOf("# GeoNames")).isEmpty())
    }

    @Test
    fun parse_tooFewColumns_isSkipped() {
        assertTrue(PlaceFileParser.parse(sequenceOf("Testtown\tIT\t45.5\t10.5")).isEmpty())
    }

    @Test
    fun parse_tooManyColumns_isSkipped() {
        assertTrue(PlaceFileParser.parse(sequenceOf("Testtown\tIT\t45.5\t10.5\t20000\textra")).isEmpty())
    }

    @Test
    fun parse_latitudeNotANumber_isSkipped() {
        assertTrue(PlaceFileParser.parse(sequenceOf("Testtown\tIT\tnorth\t10.5\t20000")).isEmpty())
    }

    @Test
    fun parse_longitudeNotANumber_isSkipped() {
        assertTrue(PlaceFileParser.parse(sequenceOf("Testtown\tIT\t45.5\teast\t20000")).isEmpty())
    }

    @Test
    fun parse_populationNotANumber_isSkipped() {
        assertTrue(PlaceFileParser.parse(sequenceOf("Testtown\tIT\t45.5\t10.5\tmany")).isEmpty())
    }

    @Test
    fun label_addsCountryName() {
        assertEquals("Testtown, Italy", Place("Testtown", "IT", 45.5, 10.5, 20_000).label())
    }
}
