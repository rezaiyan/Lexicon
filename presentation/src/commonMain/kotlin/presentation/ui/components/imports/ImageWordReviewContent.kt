package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_wizard_discard
import lexicon.resources.generated.resources.ai_wizard_discard_message
import lexicon.resources.generated.resources.ai_wizard_discard_title
import lexicon.resources.generated.resources.ai_wizard_keep
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.image_review_add_words
import lexicon.resources.generated.resources.image_review_description_label
import lexicon.resources.generated.resources.image_review_edit_word
import lexicon.resources.generated.resources.image_review_subtitle
import lexicon.resources.generated.resources.image_review_title
import lexicon.resources.generated.resources.image_review_translation_label
import lexicon.resources.generated.resources.image_review_word_label
import lexicon.resources.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun ImageWordReviewContent(
    reviewState: ImageReviewState.Review,
    onRemoveWord: (Int) -> Unit,
    onStartEditWord: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (Int, String, String, String) -> Unit,
    onConfirmImport: () -> Unit,
    onRequestCancel: () -> Unit,
    onDismissCancelConfirmation: () -> Unit,
    onCancelImport: () -> Unit,
) {
    val words = reviewState.words
    val spacing = Theme.spacing

    // Edit dialog
    val editingWord = words.firstOrNull { it.id == reviewState.editingWordId }
    if (editingWord != null) {
        WordEditDialog(
            word = editingWord,
            onSave = { w, t, d -> onSaveEdit(editingWord.id, w, t, d) },
            onDismiss = onCancelEdit,
        )
    }

    // Cancel confirmation dialog
    if (reviewState.showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissCancelConfirmation,
            title = { Text(stringResource(Res.string.ai_wizard_discard_title)) },
            text = { Text(stringResource(Res.string.ai_wizard_discard_message)) },
            confirmButton = {
                TextButton(onClick = onCancelImport) {
                    Text(stringResource(Res.string.ai_wizard_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCancelConfirmation) {
                    Text(stringResource(Res.string.ai_wizard_keep))
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.md,
                vertical = spacing.sm,
            )
        ) {
            Text(
                text = stringResource(Res.string.image_review_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                text = stringResource(Res.string.image_review_subtitle, words.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Word list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(words, key = { it.id }) { item ->
                ExtractedWordCard(
                    item = item,
                    onEdit = { onStartEditWord(item.id) },
                    onRemove = { onRemoveWord(item.id) },
                )
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            OutlinedButton(
                onClick = onRequestCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.cancel))
            }

            Button(
                onClick = onConfirmImport,
                modifier = Modifier.weight(2f),
                enabled = words.isNotEmpty() && !reviewState.isImporting,
            ) {
                if (reviewState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Theme.spacing.xxxs,
                    )
                } else {
                    Text(stringResource(Res.string.image_review_add_words, words.size))
                }
            }
        }
    }
}

@Composable
private fun ExtractedWordCard(
    item: ExtractedWordItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = Theme.spacing

    Card(
        shape = RoundedCornerShape(Theme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.md, end = spacing.xs, top = spacing.xs, bottom = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                )
            }
        }
    }
}

@Composable
private fun WordEditDialog(
    word: ExtractedWordItem,
    onSave: (word: String, translation: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editWord by remember(word.id) { mutableStateOf(word.word) }
    var editTranslation by remember(word.id) { mutableStateOf(word.translation) }
    var editDescription by remember(word.id) { mutableStateOf(word.description) }
    val spacing = Theme.spacing

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.image_review_edit_word)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = editWord,
                    onValueChange = { editWord = it },
                    label = { Text(stringResource(Res.string.image_review_word_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editTranslation,
                    onValueChange = { editTranslation = it },
                    label = { Text(stringResource(Res.string.image_review_translation_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    label = { Text(stringResource(Res.string.image_review_description_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(editWord.trim(), editTranslation.trim(), editDescription.trim()) },
                enabled = editWord.isNotBlank() && editTranslation.isNotBlank(),
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
