package net.drehtuer.nextgallery.core.model

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -MAX_LATITUDE..MAX_LATITUDE) { "latitude out of range: $latitude" }
        require(longitude in -MAX_LONGITUDE..MAX_LONGITUDE) { "longitude out of range: $longitude" }
    }

    private companion object {
        const val MAX_LATITUDE = 90.0
        const val MAX_LONGITUDE = 180.0
    }
}
