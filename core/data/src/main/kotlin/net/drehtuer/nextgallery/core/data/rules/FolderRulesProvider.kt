package net.drehtuer.nextgallery.core.data.rules

import net.drehtuer.nextgallery.core.model.FolderRule
import javax.inject.Inject

/** Supplies the current folder rules. Phase 4 replaces the default with user settings. */
fun interface FolderRulesProvider {
    suspend fun current(): List<FolderRule>
}

class DefaultFolderRulesProvider @Inject constructor() : FolderRulesProvider {
    override suspend fun current(): List<FolderRule> = DEFAULT_RULES

    companion object {
        val DEFAULT_RULES = listOf(
            FolderRule("Android/media/com.whatsapp/WhatsApp/Media/.Statuses", FolderRule.Type.EXCLUDE),
            FolderRule("Android/data", FolderRule.Type.EXCLUDE),
        )
    }
}
