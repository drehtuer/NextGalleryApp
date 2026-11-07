package net.drehtuer.nextgallery.feature.map

import org.osmdroid.config.IConfigurationProvider
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File

/** How the app uses osmdroid. Kept apart from the renderer, so it can be checked without drawing a map. */
internal object OsmdroidSetup {
    /** OpenStreetMap's tile policy asks for a name that identifies the app. It carries no personal data. */
    const val USER_AGENT = "NextGalleryApp (Android, osmdroid)"
    const val MIN_ZOOM = 0.0
    const val MAX_ZOOM = 19.0
    private const val TILE_CACHE_MAX_BYTES = 200L * 1024 * 1024
    private const val TILE_CACHE_TRIM_BYTES = 150L * 1024 * 1024

    /** Must run before a [MapView] is created. The cache stays in the app's cache folder. */
    fun configure(configuration: IConfigurationProvider, cacheDir: File) {
        configuration.userAgentValue = USER_AGENT
        configuration.osmdroidBasePath = File(cacheDir, "osmdroid")
        configuration.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        configuration.tileFileSystemCacheMaxBytes = TILE_CACHE_MAX_BYTES
        configuration.tileFileSystemCacheTrimBytes = TILE_CACHE_TRIM_BYTES
    }

    /** D44: zoom from the whole globe to OpenStreetMap's maximum, a repeating world, pinch and two-finger drag. */
    fun configure(view: MapView) {
        view.minZoomLevel = MIN_ZOOM
        view.maxZoomLevel = MAX_ZOOM
        view.isHorizontalMapRepetitionEnabled = true
        view.isVerticalMapRepetitionEnabled = false
        view.setMultiTouchControls(true)
        view.isTilesScaledToDpi = true
        // The screen has its own zoom buttons.
        view.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    }
}

/** A tile server address with the placeholders `{z}`, `{x}` and `{y}` (D23). */
internal class TemplateTileSource(private val template: String) :
    OnlineTileSourceBase(
        // osmdroid keeps its cache per source name, so that two servers do not mix their tiles.
        "tiles-${template.hashCode()}",
        OsmdroidSetup.MIN_ZOOM.toInt(),
        OsmdroidSetup.MAX_ZOOM.toInt(),
        TILE_SIZE_PX,
        "",
        arrayOf(template),
    ) {
    override fun getTileURLString(pMapTileIndex: Long): String = template
        .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
        .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
        .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())

    private companion object {
        const val TILE_SIZE_PX = 256
    }
}
