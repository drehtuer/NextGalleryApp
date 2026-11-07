package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.Place
import java.util.Locale

/** Reads the bundled place list: tab-separated name, country code, latitude, longitude, population. */
object PlaceFileParser {
    private const val COLUMN_COUNT = 5
    private const val LATITUDE_COLUMN = 2
    private const val LONGITUDE_COLUMN = 3
    private const val POPULATION_COLUMN = 4
    private const val COMMENT_PREFIX = "#"

    /** Comments and malformed lines are skipped. */
    fun parse(lines: Sequence<String>): List<Place> = lines
        .filterNot { it.startsWith(COMMENT_PREFIX) }
        .map { it.split('\t') }
        .filter { it.size == COLUMN_COUNT }
        .mapNotNull(::parseColumns)
        .toList()

    private fun parseColumns(columns: List<String>): Place? {
        val latitude = columns[LATITUDE_COLUMN].toDoubleOrNull()
        val longitude = columns[LONGITUDE_COLUMN].toDoubleOrNull()
        val population = columns[POPULATION_COLUMN].toLongOrNull()
        return if (latitude != null && longitude != null && population != null) {
            Place(columns[0], columns[1], latitude, longitude, population)
        } else {
            null
        }
    }
}

/** "Desenzano del Garda, Italy". The app is in English only (D34), so the country is too. */
fun Place.label(): String {
    val country = Locale.Builder().setRegion(countryCode).build().getDisplayCountry(Locale.ENGLISH)
    return "$name, $country"
}
