package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import feature.aiimport.model.AiWordImportUiState
import theme.AppDimensions
import theme.AppSpacing

@Composable
internal fun AiWordPreviewStep(
    state: AiWordImportUiState,
    onToggleWord: (Int) -> Unit,
    onImport: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    val scrollState = rememberScrollState()
    val selectedCount = state.selectedWordIndices.size
    val totalCount = state.suggestedWords.size
    val allSelected = selectedCount == totalCount && totalCount > 0

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.md)
                .padding(bottom = spacing.xs)
        ) {
            Spacer(modifier = Modifier.height(spacing.small))

            PreviewHeader(totalCount, spacing)

            Spacer(modifier = Modifier.height(spacing.extraSmall2))

            SelectionControls(
                allSelected = allSelected,
                selectedCount = selectedCount,
                totalCount = totalCount,
                suggestedWordIndices = state.suggestedWords.indices,
                selectedWordIndices = state.selectedWordIndices,
                onToggleWord = onToggleWord,
            )

            Spacer(modifier = Modifier.height(spacing.extraSmall2))

            state.suggestedWords.forEachIndexed { index, word ->
                WordPreviewCard(
                    originalWord = word.originalWord,
                    translation = word.translation,
                    description = word.description,
                    isSelected = state.selectedWordIndices.contains(index),
                    onClick = { onToggleWord(index) },
                    spacing = spacing,
                )
            }

            state.error?.let {
                Spacer(modifier = Modifier.height(spacing.extraSmall2))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
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
private fun PreviewHeader(totalCount: Int, spacing: AppSpacing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Your personalized",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "vocabulary",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$totalCount words",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    horizontal = spacing.extraSmall,
                    vertical = spacing.extraSmall3
                )
            )
        }
    }
}

@Composable
private fun SelectionControls(
    allSelected: Boolean,
    selectedCount: Int,
    totalCount: Int,
    suggestedWordIndices: IntRange,
    selectedWordIndices: Set<Int>,
    onToggleWord: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (allSelected) "All selected" else "$selectedCount of $totalCount selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                if (allSelected) {
                    suggestedWordIndices.forEach { onToggleWord(it) }
                } else {
                    suggestedWordIndices
                        .filter { it !in selectedWordIndices }
                        .forEach { onToggleWord(it) }
                }
            }
        ) {
            Text(
                text = if (allSelected) "Deselect all" else "Select all",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
