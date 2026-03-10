package presentation.ui.components.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import feature.aiimport.model.AiWordImportUiState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_wizard_all_selected
import lexicon.resources.generated.resources.ai_wizard_deselect_all
import lexicon.resources.generated.resources.ai_wizard_preview_highlight
import lexicon.resources.generated.resources.ai_wizard_preview_title
import lexicon.resources.generated.resources.ai_wizard_select_all
import lexicon.resources.generated.resources.ai_wizard_selection_count
import org.jetbrains.compose.resources.stringResource
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

@Composable
internal fun AiWordPreviewStep(
    state: AiWordImportUiState,
    onToggleWord: (Int) -> Unit,
    onImport: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    val selectedCount = state.selectedWordIndices.size
    val totalCount = state.suggestedWords.size
    val allSelected = selectedCount == totalCount && totalCount > 0

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + selection controls (non-scrolling)
        PreviewHeader(
            selectedCount = selectedCount,
            totalCount = totalCount,
            allSelected = allSelected,
            suggestedWordIndices = state.suggestedWords.indices,
            selectedWordIndices = state.selectedWordIndices,
            onToggleWord = onToggleWord,
            spacing = spacing
        )

        // Word list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = spacing.sm
            )
        ) {
            itemsIndexed(
                items = state.suggestedWords,
                key = { index, word -> "${index}_${word.originalWord}" }
            ) { index, word ->
                WordPreviewCard(
                    index = index,
                    originalWord = word.originalWord,
                    translation = word.translation,
                    description = word.description,
                    isSelected = state.selectedWordIndices.contains(index),
                    onClick = { onToggleWord(index) },
                )
            }

            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = spacing.xs)
                    )
                }
            }
        }

        PreviewBottomBar(
            isLoading = state.isLoading,
            selectedCount = selectedCount,
            onImport = onImport,
            spacing = spacing,
            dimensions = dimensions,
        )
    }
}

@Composable
private fun PreviewHeader(
    selectedCount: Int,
    totalCount: Int,
    allSelected: Boolean,
    suggestedWordIndices: IntRange,
    selectedWordIndices: Set<Int>,
    onToggleWord: (Int) -> Unit,
    spacing: AppSpacing,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Title
        Text(
            text = stringResource(Res.string.ai_wizard_preview_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(Res.string.ai_wizard_preview_highlight),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        // Selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Count pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.spacing.xs)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = if (allSelected) stringResource(Res.string.ai_wizard_all_selected, totalCount)
                    else stringResource(Res.string.ai_wizard_selection_count, selectedCount, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Toggle chip
            FilterChip(
                selected = allSelected,
                onClick = {
                    if (allSelected) {
                        suggestedWordIndices.forEach { onToggleWord(it) }
                    } else {
                        suggestedWordIndices
                            .filter { it !in selectedWordIndices }
                            .forEach { onToggleWord(it) }
                    }
                },
                label = {
                    Text(
                        text = if (allSelected) stringResource(Res.string.ai_wizard_deselect_all) else stringResource(Res.string.ai_wizard_select_all),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = if (allSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSizeSmall)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(Theme.shapes.pill),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = allSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))
    }
}
