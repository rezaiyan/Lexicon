package presentation.ui.components.imports

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import feature.aiimport.model.AiWordImportUiState
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

@Composable
private fun WordPreviewCard(
    originalWord: String,
    translation: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    spacing: AppSpacing,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "word_card_bg"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.extraSmall2),
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) Theme.elevation.medium else Theme.elevation.none
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = originalWord,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = Theme.spacing.xxxs)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBottomBar(
    isLoading: Boolean,
    selectedCount: Int,
    onImport: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = Theme.elevation.overlay,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.md))
            if (isLoading) {
                Row(
                    modifier = Modifier.padding(vertical = spacing.small),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(spacing.extraSmall2))
                    Text(
                        text = "Adding to your library…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = onImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = Theme.dimensions.contentMaxWidth),
                    enabled = selectedCount > 0,
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = Theme.spacing.lg),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSizeMedium)
                    )
                    Spacer(modifier = Modifier.size(spacing.extraSmall2))
                    Text(
                        text = if (selectedCount > 0) "Add $selectedCount Words to Library"
                        else "Select words to add",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.md))
        }
    }
}

@Composable
internal fun DiscardConfirmationContent(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        title = "Discard suggestions?",
        message = "Your AI-generated vocabulary list will be lost.",
        primaryButton = ButtonState(
            text = "Discard",
            onClick = onDiscard,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = "Keep",
            onClick = onKeep
        ),
    )
}
