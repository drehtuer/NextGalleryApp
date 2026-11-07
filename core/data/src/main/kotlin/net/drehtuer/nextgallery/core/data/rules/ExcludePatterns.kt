package net.drehtuer.nextgallery.core.data.rules

import net.drehtuer.nextgallery.core.common.RelativePaths

/** The "Also exclude folders matching" field: one pattern per line, `*` matches any characters. */
object ExcludePatterns {
    fun parse(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** Matches a normalized relative path. Like a rule, a pattern also covers the subfolders of what it matches. */
    fun toRegex(pattern: String): Regex {
        val glob = RelativePaths.normalize(pattern.trim()).split('*').joinToString(".*") { Regex.escape(it) }
        return Regex("$glob(/.*)?", RegexOption.IGNORE_CASE)
    }
}
