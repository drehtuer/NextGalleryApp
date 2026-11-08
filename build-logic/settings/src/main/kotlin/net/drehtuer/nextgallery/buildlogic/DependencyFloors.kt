package net.drehtuer.nextgallery.buildlogic

/**
 * Lowest versions allowed for libraries that the build tools (AGP, lint, ktlint, detekt, …) pull in with known
 * vulnerabilities (D62). None of them ships in the APK; they run on the build machine. An older version is raised to
 * the floor, a newer one is left alone. Remove an entry once the tools that bring it require the fixed version.
 */
internal object DependencyFloors {
    private const val BOUNCY_CASTLE = "1.85"
    private const val LOGBACK = "1.5.34"
    private const val WIRE = "6.4.5"

    private val FLOORS = mapOf(
        // GHSA-xxph-c9ww-hj94
        "com.google.guava:guava" to "33.7.2-jre",
        // GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26, GHSA-wg6q-6289-32hp; the three modules must stay on one version.
        "org.bouncycastle:bcprov-jdk18on" to BOUNCY_CASTLE,
        "org.bouncycastle:bcpkix-jdk18on" to BOUNCY_CASTLE,
        "org.bouncycastle:bcutil-jdk18on" to BOUNCY_CASTLE,
        // GHSA-9rm7-3qhh-h2mc
        "com.squareup.wire:wire-runtime" to WIRE,
        "com.squareup.wire:wire-runtime-jvm" to WIRE,
        // GHSA-qqpg-mvqg-649v, GHSA-p47f-322f-whfh, GHSA-jhq6-gfmj-v8fx; classic and core must stay on one version.
        "ch.qos.logback:logback-core" to LOGBACK,
        "ch.qos.logback:logback-classic" to LOGBACK,
        // GHSA-3677-xxcr-wjqv
        "org.bitbucket.b_c:jose4j" to "0.9.6",
        // GHSA-2363-cqg2-863c
        "org.jdom:jdom2" to "2.0.6.1",
        // GHSA-j288-q9x7-2f5v
        "org.apache.commons:commons-lang3" to "3.18.0",
        // GHSA-7r82-7xv7-xcpj
        "org.apache.httpcomponents:httpclient" to "4.5.14",
    )

    /** The version to use instead of [version], or null if [version] is fine or the module has no floor. */
    fun raise(group: String, name: String, version: String?): String? {
        val floor = FLOORS["$group:$name"] ?: return null
        if (version.isNullOrEmpty()) return null
        return floor.takeIf { isOlder(version, floor) }
    }

    /** Compares the numeric parts (`33.4.0-jre` → 33, 4, 0); a missing part counts as 0. */
    fun isOlder(version: String, than: String): Boolean {
        val left = numbers(version)
        val right = numbers(than)
        for (i in 0 until maxOf(left.size, right.size)) {
            val difference = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (difference != 0) return difference < 0
        }
        return false
    }

    private fun numbers(version: String): List<Int> =
        version.substringBefore('-').split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
