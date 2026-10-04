package presentation.ui.components.imports

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import feature.aiimport.model.AiWordImportStep
import feature.aiimport.model.AiWordImportUiState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_n_words
import lexicon.resources.generated.resources.ai_wizard_all_selected
import lexicon.resources.generated.resources.ai_wizard_deselect_all
import lexicon.resources.generated.resources.ai_wizard_preview_highlight
import lexicon.resources.generated.resources.ai_wizard_preview_title
import lexicon.resources.generated.resources.ai_wizard_select_all
import lexicon.resources.generated.resources.ai_wizard_selection_count
import lexicon.resources.generated.resources.step_of
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Final wizard step: pick which suggested words to add. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiWordPreviewStep(
    state: AiWordImportUiState,
    onToggleWord: (Int) -> Unit,
    onTagSelected: (Long?) -> Unit,
    onImport: () -> Unit,
) {
    val selectedCount = state.selectedWordIndices.size
    val totalCount = state.suggestedWords.size
    val allSelected = selectedCount == totalCount && totalCount > 0

    SheetPage(
        eyebrow = stringResource(Res.string.step_of, AiWordImportStep.PREVIEW.ordinal + 1, AiWizardTotalSteps),
        title = stringResource(Res.string.ai_wizard_preview_title),
        highlight = stringResource(Res.string.ai_wizard_preview_highlight),
        scrollable = false,
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.add_n_words, selectedCount),
                onClick = onImport,
                enabled = selectedCount > 0,
                isLoading = state.isLoading,
            )
        },
    ) {
        val summary = listOfNotNull(
            state.selectedTargetLanguage?.displayName,
            state.selectedLevel?.let { stringResource(it.title) },
            state.selectedTopics.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        )
        if (summary.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
            ) {
                summary.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Theme.shapes.pill))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
                    )
                }
            }
        }

        TagSelectorRow(
            tags = state.tags,
            selectedTagId = state.selectedTagId,
            onTagSelected = onTagSelected,
        )

        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (allSelected) stringResource(Res.string.ai_wizard_all_selected, totalCount)
                    else stringResource(Res.string.ai_wizard_selection_count, selectedCount, totalCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val toToggle = if (allSelected) {
                            state.suggestedWords.indices.toList()
                        } else {
                            state.suggestedWords.indices.filter { it !in state.selectedWordIndices }
                        }
                        toToggle.forEach(onToggleWord)
                    },
                ) {
                    Text(
                        stringResource(
                            if (allSelected) Res.string.ai_wizard_deselect_all else Res.string.ai_wizard_select_all
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))

            LazyColumn(contentPadding = PaddingValues(bottom = Theme.spacing.xs)) {
                itemsIndexed(
                    items = state.suggestedWords,
                    key = { index, word -> "${index}_${word.originalWord}" },
                ) { index, word ->
                    SuggestedWordRow(
                        word = word.originalWord,
                        translation = word.translation,
                        description = word.description,
                        selected = index in state.selectedWordIndices,
                        showDivider = index < state.suggestedWords.lastIndex,
                        onToggle = { onToggleWord(index) },
                    )
                }
                state.error?.let { errorText ->
                    item {
                        val isNetworkError = listOf("timeout", "connect", "network", "internet")
                            .any { errorText.contains(it, ignoreCase = true) }
                        ErrorMessage(
                            if (isNetworkError) "You're offline -- check your connection and try again."
                            else errorText.ifEmpty { "Import failed -- please try again." }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedWordRow(
    word: String,
    translation: String,
    description: String,
    selected: Boolean,
    showDivider: Boolean,
    onToggle: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val boxColor by animateColorAsState(if (selected) accent else MaterialTheme.colorScheme.surface, label = "check")
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
                .padding(vertical = Theme.spacing.xs + Theme.spacing.xxxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm + Theme.spacing.xxxs),
        ) {
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSize)
                    .clip(RoundedCornerShape(Theme.shapes.small - Theme.spacing.xxxs / 2))
                    .background(boxColor)
                    .then(
                        if (selected) Modifier
                        else Modifier.border(
                            Theme.spacing.xxxs,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(Theme.shapes.small - Theme.spacing.xxxs / 2),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
            ) {
                Text(
                    word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description.isNotBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
        }
    }
}
