package net.drehtuer.nextgallery.core.model

/** A rectangle on the globe. [west] greater than [east] means that it crosses the antimeridian. */
data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double) {
    val crossesAntimeridian: Boolean
        get() = west > east

    fun contains(point: GeoPoint): Boolean {
        val insideLongitude = if (crossesAntimeridian) {
            point.longitude >= west || point.longitude <= east
        } else {
            point.longitude in west..east
        }
        return point.latitude in south..north && insideLongitude
    }

    /** For route arguments. Doubles print with enough digits to read back the same value. */
    fun encode(): String = "$south,$west,$north,$east"

    companion object {
        private const val PART_COUNT = 4

        /** Returns null if [text] did not come from [encode]. */
        fun decode(text: String): GeoBounds? = text.split(',')
            .mapNotNull { it.toDoubleOrNull() }
            .takeIf { it.size == PART_COUNT }
            ?.let { parts ->
                val (south, west, north) = parts
                GeoBounds(south, west, north, east = parts.last())
            }

        /** The smallest rectangle around [points], which must not be empty. It never crosses the antimeridian. */
        fun around(points: List<GeoPoint>) = GeoBounds(
            south = points.minOf { it.latitude },
            west = points.minOf { it.longitude },
            north = points.maxOf { it.latitude },
            east = points.maxOf { it.longitude },
        )
    }
}
