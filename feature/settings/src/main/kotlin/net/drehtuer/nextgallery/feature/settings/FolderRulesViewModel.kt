package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.rules.ExcludePatterns
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.DeviceFolder
import net.drehtuer.nextgallery.core.model.FolderRule
import javax.inject.Inject

enum class RuleChoice { INCLUDE, DEFAULT, EXCLUDE }

data class FolderRuleRow(val name: String, val relativePath: String, val itemCount: Int, val choice: RuleChoice)

sealed interface FolderRulesUiState {
    data object Loading : FolderRulesUiState

    data class Content(val rows: List<FolderRuleRow>, val patterns: String) : FolderRulesUiState
}

sealed interface FolderRulesEvent {
    data class ChoiceChanged(val relativePath: String, val choice: RuleChoice) : FolderRulesEvent

    data class PatternsChanged(val text: String) : FolderRulesEvent
}

/**
 * The folders on the device plus folders that only have a rule, such as the default excludes, so that every rule can
 * be seen and removed. Reading MediaStore can fail; the rules are then still shown.
 */
@HiltViewModel
class FolderRulesViewModel @Inject constructor(
    folderRepository: FolderRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    /** The field's own text, with blank lines and all, once the user has edited it. */
    private val editedPatterns = MutableStateFlow<String?>(null)

    val state: StateFlow<FolderRulesUiState> = combine(
        folderRepository.observeAllDeviceFolders().catch { emit(emptyList()) },
        settingsRepository.observeSettings(),
        editedPatterns,
    ) { folders, settings, edited ->
        val patterns = edited ?: settings.excludePatterns.joinToString("\n")
        FolderRulesUiState.Content(folderRuleRows(folders, settings.folderRules), patterns)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FolderRulesUiState.Loading)

    fun onEvent(event: FolderRulesEvent) {
        viewModelScope.launch {
            when (event) {
                is FolderRulesEvent.ChoiceChanged -> applyChoice(event.relativePath, event.choice)

                is FolderRulesEvent.PatternsChanged -> {
                    editedPatterns.value = event.text
                    settingsRepository.setExcludePatterns(ExcludePatterns.parse(event.text))
                }
            }
        }
    }

    private suspend fun applyChoice(relativePath: String, choice: RuleChoice) {
        when (choice) {
            RuleChoice.INCLUDE -> settingsRepository.addRule(FolderRule(relativePath, FolderRule.Type.INCLUDE))
            RuleChoice.EXCLUDE -> settingsRepository.addRule(FolderRule(relativePath, FolderRule.Type.EXCLUDE))
            RuleChoice.DEFAULT -> settingsRepository.removeRule(relativePath)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun folderRuleRows(folders: List<DeviceFolder>, rules: List<FolderRule>): List<FolderRuleRow> {
    val rulesByPath = rules.associateBy { pathKey(it.relativePath) }
    val folderPaths = folders.map { pathKey(it.relativePath) }.toSet()
    val folderRows = folders.map { folder ->
        val rule = rulesByPath[pathKey(folder.relativePath)]
        FolderRuleRow(folder.name, folder.relativePath, folder.itemCount, rule.toChoice())
    }
    val ruleOnlyRows = rules.filter { pathKey(it.relativePath) !in folderPaths }.map { rule ->
        FolderRuleRow(rule.relativePath.substringAfterLast('/'), rule.relativePath, 0, rule.toChoice())
    }
    return (folderRows + ruleOnlyRows).sortedBy { pathKey(it.relativePath) }
}

private fun pathKey(path: String) = RelativePaths.normalize(path).lowercase()

private fun FolderRule?.toChoice() = when (this?.type) {
    FolderRule.Type.INCLUDE -> RuleChoice.INCLUDE
    FolderRule.Type.EXCLUDE -> RuleChoice.EXCLUDE
    null -> RuleChoice.DEFAULT
}
