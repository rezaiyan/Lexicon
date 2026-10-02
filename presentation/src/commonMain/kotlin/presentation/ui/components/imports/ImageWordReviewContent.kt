package presentation.ui.components.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.WordFormSheetPage
import expects.BackHandler
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.edit_word_cd
import lexicon.resources.generated.resources.image_review_add_words
import lexicon.resources.generated.resources.image_review_edit_word
import lexicon.resources.generated.resources.remove_word_cd
import lexicon.resources.generated.resources.review_found_subtitle
import lexicon.resources.generated.resources.review_found_title
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** "We found N words" — edit or remove extracted words before adding them. */
@Composable
fun ImageWordReviewContent(
    reviewState: ImageReviewState.Review,
    onRemoveWord: (Int) -> Unit,
    onStartEditWord: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (Int, String, String, String) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissCancelConfirmation: () -> Unit,
    onCancelImport: () -> Unit,
) {
    val words = reviewState.words

    val editingWord = words.firstOrNull { it.id == reviewState.editingWordId }
    if (editingWord != null) {
        EditExtractedWordPage(
            word = editingWord,
            onSave = { w, t, d -> onSaveEdit(editingWord.id, w, t, d) },
            onCancel = onCancelEdit,
        )
        return
    }

    if (reviewState.showCancelConfirmation) {
        DiscardConfirmationDialog(onDiscard = onCancelImport, onKeep = onDismissCancelConfirmation)
    }

    SheetPage(
        title = stringResource(Res.string.review_found_title, words.size),
        subtitle = stringResource(Res.string.review_found_subtitle),
        scrollable = false,
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.image_review_add_words, words.size),
                onClick = onConfirmImport,
                enabled = words.isNotEmpty(),
                isLoading = reviewState.isImporting,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            contentPadding = PaddingValues(bottom = Theme.spacing.xs),
        ) {
            items(words, key = { it.id }) { item ->
                ExtractedWordCard(
                    item = item,
                    onEdit = { onStartEditWord(item.id) },
                    onRemove = { onRemoveWord(item.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ExtractedWordCard(
    item: ExtractedWordItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            Theme.dimensions.borderWidth,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
        ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
                .padding(start = Theme.spacing.md, end = Theme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = Theme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
            ) {
                Text(item.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    item.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.description.isNotBlank()) {
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit_word_cd, item.word),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(Res.string.remove_word_cd, item.word),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
            }
        }
    }
}

/** Edits one extracted word in place of the list; system back cancels the edit. */
@Composable
private fun EditExtractedWordPage(
    word: ExtractedWordItem,
    onSave: (word: String, translation: String, description: String) -> Unit,
    onCancel: () -> Unit,
) {
    var editWord by remember(word.id) { mutableStateOf(word.word) }
    var editTranslation by remember(word.id) { mutableStateOf(word.translation) }
    var editDescription by remember(word.id) { mutableStateOf(word.description) }

    BackHandler(onBack = onCancel)

    WordFormSheetPage(
        title = stringResource(Res.string.image_review_edit_word),
        word = editWord,
        onWordChange = { editWord = it },
        translation = editTranslation,
        onTranslationChange = { editTranslation = it },
        description = editDescription,
        onDescriptionChange = { editDescription = it },
        onSave = { onSave(editWord.trim(), editTranslation.trim(), editDescription.trim()) },
        onCancel = onCancel,
    )
}
