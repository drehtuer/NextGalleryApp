package net.drehtuer.nextgallery.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The D24 targets. Run on a real device with photos, screen unlocked:
 * `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class GalleryBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    private fun measure(
        metric: Metric,
        startupMode: StartupMode?,
        compilation: CompilationMode = CompilationMode.Partial(),
        journey: MacrobenchmarkScope.() -> Unit,
    ) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(metric),
        compilationMode = compilation,
        startupMode = startupMode,
        iterations = ITERATIONS,
        setupBlock = {
            grantPhotoAccess()
            pressHome()
        },
    ) { journey() }

    private fun MacrobenchmarkScope.open(then: MacrobenchmarkScope.() -> Unit) {
        startActivityAndWait()
        then()
    }

    @Test
    fun coldStart() = measure(StartupTimingMetric(), StartupMode.COLD) { startActivityAndWait() }

    @Test
    fun coldStartWithoutProfile() =
        measure(StartupTimingMetric(), StartupMode.COLD, CompilationMode.None()) { startActivityAndWait() }

    @Test
    fun folderGridScroll() = measure(FrameTimingMetric(), StartupMode.WARM) { open { scrollFolderGrid() } }

    @Test
    fun timelineFastScroll() = measure(FrameTimingMetric(), StartupMode.WARM) { open { fastScrollTimeline() } }

    @Test
    fun viewerSwipe() = measure(FrameTimingMetric(), StartupMode.WARM) {
        open {
            scrollFolderGrid()
            swipeViewer()
        }
    }

    private companion object {
        const val ITERATIONS = 5
    }
}
