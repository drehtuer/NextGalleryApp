package net.drehtuer.nextgallery.benchmark

import android.Manifest
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "net.drehtuer.nextgallery"
private const val WAIT_MILLIS = 5_000L
private const val SWIPES = 3
private const val FLINGS = 3

/** The device needs some photos. Access is granted here, because a dialog would stop the journey. */
internal fun MacrobenchmarkScope.grantPhotoAccess() {
    device.executeShellCommand("pm grant $packageName ${Manifest.permission.READ_MEDIA_IMAGES}")
}

private fun MacrobenchmarkScope.waitFor(description: String, selector: () -> UiObject2?): UiObject2 {
    device.wait(Until.hasObject(By.pkg(packageName)), WAIT_MILLIS)
    return checkNotNull(selector()) { "$description is not on the screen. Does the device have photos?" }
}

private fun MacrobenchmarkScope.grid(): UiObject2 = waitFor("A scrolling grid") {
    device.wait(Until.findObject(By.scrollable(true)), WAIT_MILLIS)
}

private fun UiObject2.flingDownAndUp() {
    setGestureMarginPercentage(GESTURE_MARGIN)
    repeat(FLINGS) { fling(Direction.DOWN) }
    repeat(FLINGS) { fling(Direction.UP) }
}

/** Starts on the folder list: opens the first folder and scrolls its grid. */
internal fun MacrobenchmarkScope.scrollFolderGrid() {
    val folders = grid()
    folders.children.first().click()
    device.waitForIdle()
    grid().flingDownAndUp()
}

/** Opens the timeline and drags the date rail's thumb, which has this description for screen readers too. */
internal fun MacrobenchmarkScope.fastScrollTimeline() {
    waitFor("The timeline tab") { device.wait(Until.findObject(By.text("Timeline")), WAIT_MILLIS) }.click()
    val thumb = waitFor("The date rail") { device.wait(Until.findObject(By.desc("Scroll position")), WAIT_MILLIS) }
    val bottom = device.displayHeight * DRAG_END_PERCENT / PERCENT
    thumb.drag(android.graphics.Point(thumb.visibleCenter.x, bottom))
    device.waitForIdle()
}

/** Starts on a grid: opens its first image and swipes through the next ones. */
internal fun MacrobenchmarkScope.swipeViewer() {
    grid().children.first().click()
    device.waitForIdle()
    val width = device.displayWidth
    val height = device.displayHeight / 2
    repeat(SWIPES) {
        device.swipe(
            width * SWIPE_FROM_PERCENT / PERCENT,
            height,
            width * SWIPE_TO_PERCENT / PERCENT,
            height,
            SWIPE_STEPS,
        )
        device.waitForIdle()
    }
}

private const val GESTURE_MARGIN = 0.2f
private const val PERCENT = 100
private const val DRAG_END_PERCENT = 85
private const val SWIPE_FROM_PERCENT = 85
private const val SWIPE_TO_PERCENT = 15
private const val SWIPE_STEPS = 20
