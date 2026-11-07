package net.drehtuer.nextgallery.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import kotlin.math.min

/** The sizes (in pixels) and colours of the map's markers; they come from `NextGalleryTheme`. */
internal data class MarkerStyle(
    val markerSizePx: Int,
    val borderPx: Float,
    val pillHeightPx: Float,
    val pillTextSizePx: Float,
    val pillPaddingPx: Float,
    val dotSizePx: Float,
    val dotRingPx: Float,
    val haloSizePx: Int,
    @param:ColorInt val accent: Int,
    @param:ColorInt val onAccent: Int,
    @param:ColorInt val placeholder: Int,
    @param:ColorInt val ring: Int,
    @param:ColorInt val halo: Int,
)

/** Draws the cover in a circle with a white border and the count in a pill at the top right (DESIGN.md). */
internal fun drawClusterMarker(style: MarkerStyle, cover: Bitmap?, countLabel: String): Bitmap {
    // The bitmap has room for the pill on every side, so that its centre stays the circle's centre.
    val overhang = style.pillHeightPx / 2
    val side = (style.markerSizePx + 2 * overhang).toInt()
    val bitmap = createBitmap(side, side)
    val canvas = Canvas(bitmap)
    val center = side / 2f
    val radius = style.markerSizePx / 2f

    val circle = Path().apply { addCircle(center, center, radius, Path.Direction.CW) }
    canvas.withClip(circle) {
        drawColor(style.placeholder)
        if (cover != null) drawBitmap(softwareCopy(cover), centerSquare(cover), circleBounds(center, radius), null)
    }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = style.borderPx
        color = style.ring
    }
    canvas.drawCircle(center, center, radius - style.borderPx / 2, border)
    drawCountPill(canvas, style, countLabel, right = center + radius + overhang, top = 0f)
    return bitmap
}

/** A 20 dp accent disc with a 3 dp white ring on a 48 dp accent halo (DESIGN.md). */
internal fun drawLocationDot(style: MarkerStyle): Bitmap {
    val bitmap = createBitmap(style.haloSizePx, style.haloSizePx)
    val canvas = Canvas(bitmap)
    val center = style.haloSizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = style.halo
    canvas.drawCircle(center, center, center, paint)
    paint.color = style.ring
    canvas.drawCircle(center, center, style.dotSizePx / 2, paint)
    paint.color = style.accent
    canvas.drawCircle(center, center, style.dotSizePx / 2 - style.dotRingPx, paint)
    return bitmap
}

private fun drawCountPill(canvas: Canvas, style: MarkerStyle, label: String, right: Float, top: Float) {
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.onAccent
        textSize = style.pillTextSizePx
        textAlign = Paint.Align.CENTER
    }
    val width = maxOf(text.measureText(label) + 2 * style.pillPaddingPx, style.pillHeightPx)
    val pill = RectF(right - width, top, right, top + style.pillHeightPx)
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accent }
    canvas.drawRoundRect(pill, style.pillHeightPx / 2, style.pillHeightPx / 2, background)
    val baseline = pill.centerY() - (text.ascent() + text.descent()) / 2
    canvas.drawText(label, pill.centerX(), baseline, text)
}

private fun centerSquare(bitmap: Bitmap): Rect {
    val side = min(bitmap.width, bitmap.height)
    val left = (bitmap.width - side) / 2
    val top = (bitmap.height - side) / 2
    return Rect(left, top, left + side, top + side)
}

private fun circleBounds(center: Float, radius: Float) =
    RectF(center - radius, center - radius, center + radius, center + radius)

/** A software canvas cannot draw hardware bitmaps, which the thumbnail fallback can return. */
private fun softwareCopy(bitmap: Bitmap): Bitmap =
    if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
