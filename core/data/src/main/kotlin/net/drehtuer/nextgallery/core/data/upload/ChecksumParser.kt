package net.drehtuer.nextgallery.core.data.upload

/** One checksum the server reported for a file. */
data class RemoteChecksum(val algorithm: ChecksumAlgorithm, val value: String)

/** Reads the raw `oc:checksums` value, e.g. "SHA1:ab12 MD5:cd34 ADLER32:ef56". */
object ChecksumParser {
    private const val SEPARATOR = ':'
    private val WHITESPACE = Regex("\\s+")

    /** The checksums the app can compute, best first. Other algorithms and malformed parts are left out. */
    fun parse(raw: String?): List<RemoteChecksum> {
        if (raw.isNullOrBlank()) return emptyList()
        val parts = raw.trim().split(WHITESPACE).associate { part ->
            part.substringBefore(SEPARATOR, "").uppercase() to part.substringAfter(SEPARATOR, "")
        }
        return ChecksumAlgorithm.entries.mapNotNull { algorithm ->
            parts[algorithm.serverName]?.takeIf { it.isNotEmpty() }?.let { RemoteChecksum(algorithm, it) }
        }
    }
}
