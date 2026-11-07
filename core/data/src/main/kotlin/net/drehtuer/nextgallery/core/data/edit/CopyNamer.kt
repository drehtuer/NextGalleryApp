package net.drehtuer.nextgallery.core.data.edit

/** The file name of an edited copy: `IMG_1234.jpg` becomes `IMG_1234_edited.jpg`, then `IMG_1234_edited_2.jpg`. */
object CopyNamer {
    private const val SUFFIX = "_edited"
    private val EARLIER_COPY = Regex("$SUFFIX(_\\d+)?$")

    fun nameFor(displayName: String, existingNames: Set<String>): String {
        val dot = displayName.lastIndexOf('.')
        val stem = if (dot > 0) displayName.substring(0, dot) else displayName
        val extension = if (dot > 0) displayName.substring(dot) else ""
        val base = stem.replace(EARLIER_COPY, "") + SUFFIX
        val taken = existingNames.map { it.lowercase() }.toSet()
        return generateSequence(1) { it + 1 }
            .map { number -> if (number == 1) "$base$extension" else "${base}_$number$extension" }
            .first { it.lowercase() !in taken }
    }
}
