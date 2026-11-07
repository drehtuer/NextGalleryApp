package net.drehtuer.nextgallery.core.common

import java.io.File
import java.io.IOException
import java.nio.file.Files

/** For temporary files: a file that cannot be removed now is removed by the next cleanup, so this never fails. */
fun File.deleteIfPossible() {
    try {
        Files.deleteIfExists(toPath())
    } catch (_: IOException) {
        // The next cleanup removes it.
    }
}
