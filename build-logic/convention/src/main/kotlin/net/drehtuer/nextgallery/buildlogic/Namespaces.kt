package net.drehtuer.nextgallery.buildlogic

/** Android namespaces are derived from the Gradle project path, e.g. `:core:database` → `net.drehtuer.nextgallery.core.database`. */
object Namespaces {
    const val ROOT = "net.drehtuer.nextgallery"

    fun forProject(path: String): String = if (path == ":app") ROOT else ROOT + path.replace(':', '.')
}
