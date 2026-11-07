package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class Matrix3Test {
    private fun assertPoint(x: Double, y: Double, point: PlanePoint) {
        assertEquals(x, point.x, 1e-9)
        assertEquals(y, point.y, 1e-9)
    }

    @Test
    fun map_scaleAndTranslate() {
        val matrix = Matrix3(2.0, 0.0, 1.0, 0.0, 3.0, -1.0)

        assertPoint(3.0, 2.0, matrix.map(1.0, 1.0))
    }

    @Test
    fun then_appliesThisMatrixFirst() {
        val combined = Matrix3.scale(2.0, 2.0).then(Matrix3(1.0, 0.0, 10.0, 0.0, 1.0, 0.0))

        assertPoint(12.0, 2.0, combined.map(1.0, 1.0))
    }

    @Test
    fun averageScale_uniformScale_isTheScale() {
        assertEquals(3.0, Matrix3.scale(3.0, 3.0).averageScale(), 1e-9)
    }

    @Test
    fun averageScale_quarterTurn_staysOne() {
        assertEquals(1.0, GeometryMapper.rotation(1).averageScale(), 1e-9)
    }
}
