package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.FolderRule

/** One stored string per rule: the type, a separator and the path. */
internal object FolderRuleCodec {
    private const val SEPARATOR = ':'

    fun encode(rule: FolderRule): String = "${rule.type.name}$SEPARATOR${rule.relativePath}"

    /** Returns null for a value this version cannot read, so that one bad rule does not lose the others. */
    fun decode(value: String): FolderRule? {
        val type = FolderRule.Type.entries.find { it.name == value.substringBefore(SEPARATOR) }
        return type?.let { FolderRule(value.substringAfter(SEPARATOR), it) }
    }
}
