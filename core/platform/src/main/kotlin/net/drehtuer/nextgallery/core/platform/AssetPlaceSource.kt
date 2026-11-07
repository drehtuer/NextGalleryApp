package net.drehtuer.nextgallery.core.platform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.map.PlaceFileParser
import net.drehtuer.nextgallery.core.data.map.PlaceSource
import net.drehtuer.nextgallery.core.model.Place
import java.io.IOException
import javax.inject.Inject

/** Reads the place list bundled with the app (D45); `tools/build-places-asset.sh` creates it. */
class AssetPlaceSource(private val context: Context, private val assetPath: String) : PlaceSource {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, ASSET_PATH)

    override suspend fun load(): List<Place> = try {
        context.assets.open(assetPath).bufferedReader().useLines(PlaceFileParser::parse)
    } catch (_: IOException) {
        emptyList()
    }

    private companion object {
        const val ASSET_PATH = "geonames/cities15000.tsv"
    }
}
