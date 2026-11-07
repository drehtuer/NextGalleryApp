package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.Cluster
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Groups images into square cells of about `cellPx` map pixels. The grid is fixed to the world, so a
 * cluster keeps its images while the map pans and only changes when the zoom does.
 */
object GridClusterer {
    private const val TILE_SIZE_PX = 256.0
    private const val MAX_MERCATOR_LATITUDE = 85.05112878
    private const val MERCATOR_MIDDLE = 0.5
    private const val MERCATOR_RANGE = 4 * PI
    private const val FULL_CIRCLE_DEGREES = 360.0
    private const val HALF_CIRCLE_DEGREES = 180.0

    /** Returns the clusters whose centre lies in [bounds]. */
    fun cluster(points: List<GeotaggedImage>, bounds: GeoBounds, zoom: Int, cellPx: Int): List<Cluster> {
        val worldPx = TILE_SIZE_PX * 2.0.pow(zoom)
        val cellsPerSide = max(1L, (worldPx / cellPx).roundToLong())
        return points
            .groupBy { cellKey(it.location, cellsPerSide) }
            .values
            .map { Cluster(center(it), it) }
            .filter { bounds.contains(it.center) }
    }

    private fun cellKey(point: GeoPoint, cellsPerSide: Long): Long =
        cell(mercatorX(point), cellsPerSide) * cellsPerSide + cell(mercatorY(point), cellsPerSide)

    private fun cell(fraction: Double, cellsPerSide: Long): Long =
        min(cellsPerSide - 1, floor(fraction * cellsPerSide).toLong())

    /** 0 at 180° W, 1 at 180° E. */
    private fun mercatorX(point: GeoPoint): Double = (point.longitude + HALF_CIRCLE_DEGREES) / FULL_CIRCLE_DEGREES

    /** 0 at the top of the map, 1 at the bottom. */
    private fun mercatorY(point: GeoPoint): Double {
        val latitude = point.latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
        val sine = sin(Math.toRadians(latitude))
        return (MERCATOR_MIDDLE - ln((1 + sine) / (1 - sine)) / MERCATOR_RANGE).coerceIn(0.0, 1.0)
    }

    /** Longitudes are averaged on the circle, so 179° E and 179° W average to 180°, not to 0°. */
    private fun center(items: List<GeotaggedImage>): GeoPoint {
        val latitude = items.sumOf { it.location.latitude } / items.size
        val x = items.sumOf { cos(Math.toRadians(it.location.longitude)) }
        val y = items.sumOf { sin(Math.toRadians(it.location.longitude)) }
        return GeoPoint(latitude, Math.toDegrees(atan2(y, x)))
    }
}
