package net.drehtuer.nextgallery.core.common

/**
 * Helpers for MediaStore relative paths such as `DCIM/Camera/`. Comparisons ignore case, because shared
 * storage on Android is case-insensitive.
 */
object RelativePaths {
    fun normalize(path: String): String = path.split('/').filter { it.isNotEmpty() }.joinToString("/")

    fun isUnder(path: String, parent: String): Boolean {
        val child = normalize(path).lowercase()
        val ancestor = normalize(parent).lowercase()
        return ancestor.isEmpty() || child == ancestor || child.startsWith("$ancestor/")
    }
}
