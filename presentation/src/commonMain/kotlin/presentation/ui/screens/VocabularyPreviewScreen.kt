package presentation.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import presentation.model.VocabularyPreviewUiState
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

@Composable
fun VocabularyPreviewScreen(
    state: VocabularyPreviewUiState,
    onToggleWord: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onProceed: () -> Unit,
    onSkip: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.small)
    ) {
        Spacer(modifier = Modifier.height(spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Text(
            text = "PREVIEW MODE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        Text(
            text = "Vocabulary Preview",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Review these ${state.words.size} words to get started. You can edit this set later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.extraSmall4)
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(dimensions.cardCornerRadius),
                border = BorderStroke(
                    dimensions.borderWidth,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = spacing.extraSmall2,
                        vertical = spacing.extraSmall3
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.words.size} WORDS FOUND",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(spacing.extraSmall3))
                Text(
                    text = "Sort",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onSelectAll) {
                Text("Select All", color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onDeselectAll) {
                Text("Deselect All", color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
        ) {
            itemsIndexed(state.words) { index, word ->
                VocabularyWordCard(
                    word = word.originalWord,
                    translation = word.translation,
                    categoryLabel = word.sourceLanguage.uppercase().take(12),
                    isSelected = state.selectedIndices.contains(index),
                    onClick = { onToggleWord(index) },
                    spacing = spacing,
                    dimensions = dimensions
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.small))

        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedCount > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
            Spacer(modifier = Modifier.width(spacing.extraSmall2))
            Text("Get Started")
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(
                dimensions.borderWidth,
                MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
            Spacer(modifier = Modifier.width(spacing.extraSmall2))
            Text("Skip and Create My Own")
        }

        Spacer(modifier = Modifier.height(spacing.small))
    }
}

@Composable
private fun VocabularyWordCard(
    word: String,
    translation: String,
    categoryLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(dimensions.cardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.extraSmall2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(dimensions.cardCornerRadius / 2))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(
                        horizontal = spacing.extraSmall2,
                        vertical = spacing.extraSmall4
                    )
                )
            }
        }
    }
}
