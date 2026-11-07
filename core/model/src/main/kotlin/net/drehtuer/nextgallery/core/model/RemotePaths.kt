package net.drehtuer.nextgallery.core.model

/** Decoded paths on the server. The root is "/"; other paths start with "/" and have no trailing slash. */
object RemotePaths {
    const val ROOT = "/"
    private const val SEPARATOR = '/'
    private val FORBIDDEN_CHARACTERS = charArrayOf(SEPARATOR, '\\')
    private val DOT_NAMES = setOf(".", "..")

    fun normalize(path: String): String {
        val segments = segments(path)
        return if (segments.isEmpty()) ROOT else segments.joinToString("") { "$SEPARATOR$it" }
    }

    fun segments(path: String): List<String> = path.split(SEPARATOR).filter { it.isNotEmpty() }

    fun parent(path: String): String = normalize(segments(path).dropLast(1).joinToString(SEPARATOR.toString()))

    fun name(path: String): String = segments(path).lastOrNull().orEmpty()

    fun join(parent: String, name: String): String = normalize("$parent$SEPARATOR$name")

    /** A single file or folder name: not blank, not a dot name, without a path separator or control character. */
    fun isValidName(name: String): Boolean =
        name.isNotBlank() && name !in DOT_NAMES && name.none { it in FORBIDDEN_CHARACTERS || it.isISOControl() }

    /** The folders from the root down to [path], including both. */
    fun ancestors(path: String): List<String> =
        segments(path).runningFold(ROOT) { parent, segment -> join(parent, segment) }
}
