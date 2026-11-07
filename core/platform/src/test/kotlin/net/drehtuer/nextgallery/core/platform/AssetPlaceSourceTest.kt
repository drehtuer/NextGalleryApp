package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetPlaceSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun load_bundledAsset_parsesEveryRow() = runTest {
        assertEquals(EXPECTED_ROWS, AssetPlaceSource(context).load().size)
    }

    @Test
    fun load_bundledAsset_containsTheTownsInTheDesign() = runTest {
        val names = AssetPlaceSource(context).load().map { it.name }

        assertTrue("Bolzano" in names && "Desenzano del Garda" in names)
    }

    @Test
    fun load_missingAsset_returnsNothing() = runTest {
        assertTrue(AssetPlaceSource(context, "geonames/missing.tsv").load().isEmpty())
    }

    private companion object {
        /** What `core/platform/tools/build-places-asset.sh` printed for the committed file. */
        const val EXPECTED_ROWS = 34_204
    }
}
