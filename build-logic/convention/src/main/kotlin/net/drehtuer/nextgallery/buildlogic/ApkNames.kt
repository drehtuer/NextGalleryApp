package net.drehtuer.nextgallery.buildlogic

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** File names of the APKs the build hands out, instead of AGP's `app-release.apk` and `app-debug.apk`. */
internal object ApkNames {
    private const val PREFIX = "NextGallery"

    /** UTC, so that builds on different machines and in CI sort the same way. */
    private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

    /** `NextGallery-1.2.3.apk`; an unsigned build says so, because Android will not install it. */
    fun release(version: String, signed: Boolean): String =
        if (signed) "$PREFIX-$version.apk" else "$PREFIX-$version-unsigned.apk"

    /** `NextGallery-debug-1.2.3-20261107-221500.apk`: debug builds of one version differ only by their build time. */
    fun debug(version: String, builtAt: Instant): String = "$PREFIX-debug-$version-${TIMESTAMP.format(builtAt)}.apk"

    /**
     * Copies [apk] into [directory] as [fileName]. APKs left there by earlier builds are removed first, so the
     * directory always holds just the latest one and timestamped debug builds do not pile up.
     */
    fun copy(apk: File, directory: File, fileName: String): File {
        directory.mkdirs()
        directory.listFiles { file -> file.isFile && file.name.endsWith(".apk") }?.forEach { it.delete() }
        return apk.copyTo(File(directory, fileName))
    }
}
