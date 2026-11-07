package net.drehtuer.nextgallery.core.data.upload

import java.io.InputStream
import java.security.MessageDigest

/** The algorithms of `oc:checksums` the app can compute, best first. */
enum class ChecksumAlgorithm(val serverName: String, val digestName: String) {
    SHA1("SHA1", "SHA-1"),
    MD5("MD5", "MD5"),
}

/**
 * Streams a file through a digest, so that the size of an image does not matter. SHA-1 and MD5 are what the server
 * reports; here they only tell whether two copies of the user's own file are the same, so their weakness is no concern.
 */
object ChecksumCalculator {
    private const val BUFFER_BYTES = 64 * 1024

    fun sha1(stream: InputStream): String = digest(ChecksumAlgorithm.SHA1, stream)

    fun md5(stream: InputStream): String = digest(ChecksumAlgorithm.MD5, stream)

    /** Lower-case hex. */
    fun digest(algorithm: ChecksumAlgorithm, stream: InputStream): String {
        val digest = MessageDigest.getInstance(algorithm.digestName)
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHexString()
    }
}
