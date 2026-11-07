package net.drehtuer.nextgallery.buildlogic

/** The app's version, read from a Git tag such as `v1.2.3`. */
internal data class AppVersion(val name: String, val code: Int) {
    companion object {
        /** Used when there is no tag to read, for example in a shallow clone or a Git worktree in a container. */
        const val FALLBACK_TAG = "v0.1.0"
        private val TAG = Regex("""v(\d{1,4})\.(\d{1,3})\.(\d{1,3})""")
        private const val MAJOR_STEP = 1_000_000
        private const val MINOR_STEP = 1_000

        /** Android's limit for a version code. */
        private const val MAX_CODE = 2_100_000_000

        /** Every later version has a higher code, as Android requires for an update: `v1.2.3` is 1002003. */
        fun fromTag(tag: String): AppVersion {
            val (major, minor, patch) = requireNotNull(TAG.matchEntire(tag)) {
                "The version tag \"$tag\" is not of the form vMAJOR.MINOR.PATCH, with at most 3 digits after MAJOR."
            }.destructured
            val code = major.toInt() * MAJOR_STEP + minor.toInt() * MINOR_STEP + patch.toInt()
            require(code in 1..MAX_CODE) { "The version tag \"$tag\" gives the version code $code, out of range." }
            return AppVersion("$major.$minor.$patch", code)
        }
    }
}
