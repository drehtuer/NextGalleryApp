package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

private const val PATTERN_FIELD_MIN_LINES = 3

@Composable
fun FolderRulesScreen(
    state: FolderRulesUiState,
    onEvent: (FolderRulesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(stringResource(R.string.feature_settings_folders), onBack, modifier) { contentModifier ->
        when (state) {
            FolderRulesUiState.Loading -> LoadingIndicator(contentModifier)
            is FolderRulesUiState.Content -> FolderRulesList(state, onEvent, contentModifier)
        }
    }
}

@Composable
private fun FolderRulesList(
    state: FolderRulesUiState.Content,
    onEvent: (FolderRulesEvent) -> Unit,
    modifier: Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    LazyColumn(
        modifier = modifier.imePadding(),
        contentPadding = PaddingValues(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingLarge),
    ) {
        item { Text(stringResource(R.string.feature_settings_rules_intro)) }
        items(state.rows, key = { it.relativePath }) { row ->
            FolderRuleItem(row, onChoice = { onEvent(FolderRulesEvent.ChoiceChanged(row.relativePath, it)) })
        }
        item { SettingsNote(stringResource(R.string.feature_settings_rules_nomedia_note)) }
        item {
            OutlinedTextField(
                value = state.patterns,
                onValueChange = { onEvent(FolderRulesEvent.PatternsChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_settings_patterns_label)) },
                supportingText = { Text(stringResource(R.string.feature_settings_patterns_hint)) },
                minLines = PATTERN_FIELD_MIN_LINES,
            )
        }
    }
}

@Composable
private fun FolderRuleItem(row: FolderRuleRow, onChoice: (RuleChoice) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall)) {
        Column {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (row.itemCount > 0) {
                    pluralStringResource(
                        R.plurals.feature_settings_folder_detail,
                        row.itemCount,
                        row.relativePath,
                        row.itemCount,
                    )
                } else {
                    row.relativePath
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
        SegmentedChoice(
            options = RuleChoice.entries,
            selected = row.choice,
            onSelect = onChoice,
            label = { stringResource(it.label) },
        )
    }
}

private val RuleChoice.label: Int
    get() = when (this) {
        RuleChoice.INCLUDE -> R.string.feature_settings_rule_include
        RuleChoice.DEFAULT -> R.string.feature_settings_rule_default
        RuleChoice.EXCLUDE -> R.string.feature_settings_rule_exclude
    }
