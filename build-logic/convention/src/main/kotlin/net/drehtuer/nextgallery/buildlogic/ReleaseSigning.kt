package net.drehtuer.nextgallery.buildlogic

import java.io.StringReader
import java.util.Properties

/** The contents of the local `keystore.properties`, which is never committed. */
internal data class ReleaseSigning(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
) {
    /** Keeps the passwords out of build logs. */
    override fun toString() = "ReleaseSigning(storeFile=$storeFile, keyAlias=$keyAlias)"

    companion object {
        const val FILE_NAME = "keystore.properties"

        /** No file means an unsigned release build; a file with a value missing is a mistake that must not go unseen. */
        fun parse(fileContents: String?): ReleaseSigning? {
            if (fileContents == null) return null
            val properties = Properties().apply { load(StringReader(fileContents)) }
            fun value(key: String): String = requireNotNull(properties.getProperty(key)?.takeIf { it.isNotBlank() }) {
                "$FILE_NAME has no value for \"$key\"."
            }
            return ReleaseSigning(value("storeFile"), value("storePassword"), value("keyAlias"), value("keyPassword"))
        }
    }
}
