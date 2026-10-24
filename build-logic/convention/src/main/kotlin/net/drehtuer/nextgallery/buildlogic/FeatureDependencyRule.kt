package net.drehtuer.nextgallery.buildlogic

/** Feature modules must not depend on each other; cross-feature navigation is wired in `:app`. */
object FeatureDependencyRule {
    private const val FEATURE_PREFIX = ":feature:"

    fun isForbidden(fromPath: String, toPath: String): Boolean =
        fromPath != toPath && fromPath.startsWith(FEATURE_PREFIX) && toPath.startsWith(FEATURE_PREFIX)
}
