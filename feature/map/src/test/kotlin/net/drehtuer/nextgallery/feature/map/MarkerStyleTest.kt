package net.drehtuer.nextgallery.feature.map

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MarkerStyleTest {
    private val style = testMarkerStyle

    private fun solid(color: Int): Bitmap = createBitmap(20, 40).apply { eraseColor(color) }

    /** The middle of the bitmap, which is the middle of the circle. */
    private fun Bitmap.center() = getPixel(width / 2, height / 2)

    @Test
    fun clusterMarker_hasRoomForThePillOnEverySide() {
        assertEquals(82, drawClusterMarker(style, null, "1").width)
    }

    @Test
    fun clusterMarker_withoutCover_showsPlaceholder() {
        assertEquals(style.placeholder, drawClusterMarker(style, null, "1").center())
    }

    @Test
    fun clusterMarker_withCover_showsCover() {
        assertEquals(Color.GREEN, drawClusterMarker(style, solid(Color.GREEN), "1").center())
    }

    @Test
    fun clusterMarker_edgeOfCircle_isTheBorder() {
        val bitmap = drawClusterMarker(style, solid(Color.GREEN), "1")

        // The left edge of the circle, half a border in.
        assertEquals(style.ring, bitmap.getPixel(11 + 2, bitmap.height / 2))
    }

    @Test
    fun clusterMarker_outsideCircle_isTransparent() {
        assertEquals(Color.TRANSPARENT, drawClusterMarker(style, null, "1").getPixel(0, 81))
    }

    @Test
    fun clusterMarker_topRightCorner_hasAccentPill() {
        val bitmap = drawClusterMarker(style, null, "1")

        assertEquals(style.accent, bitmap.getPixel(bitmap.width - 4, 11))
    }

    @Test
    fun locationDot_isAsLargeAsTheHalo() {
        assertEquals(48, drawLocationDot(style).width)
    }

    @Test
    fun locationDot_middle_isAccent() {
        assertEquals(style.accent, drawLocationDot(style).center())
    }

    @Test
    fun locationDot_ring_isWhite() {
        val bitmap = drawLocationDot(style)

        // The disc has a radius of 10 with a ring of 3 inside it.
        assertEquals(style.ring, bitmap.getPixel(24 + 8, 24))
    }

    @Test
    fun locationDot_outsideTheDisc_isHalo() {
        val bitmap = drawLocationDot(style)

        assertEquals(style.halo, bitmap.getPixel(24 + 15, 24))
    }
}
