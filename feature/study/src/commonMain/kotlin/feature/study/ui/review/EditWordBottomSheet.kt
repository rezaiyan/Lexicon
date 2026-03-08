package feature.study.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import overlay.OverlayNavigator
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.delete_word_message
import lexicon.resources.generated.resources.delete_word_title
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.edit_word
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.translation_label

private enum class EditWordPage { Edit, ConfirmDelete }

/**
 * Content for editing a word, intended to be hosted inside
 * an [overlay.bottomsheet.BottomSheetOverlay] via OverlayHost.
 */
@Composable
fun EditWordSheetContent(
    word: Word,
    navigator: OverlayNavigator,
    onSave: (Word) -> Unit,
    onDelete: () -> Unit
) {
    var originalWord by remember { mutableStateOf(word.originalWord) }
    var translation by remember { mutableStateOf(word.translation) }
    var description by remember { mutableStateOf(word.description) }
    val focusManager = LocalFocusManager.current
    val isSaveEnabled = originalWord.isNotBlank() && translation.isNotBlank()
    val pages = rememberBottomSheetPageNavigator(EditWordPage.Edit)

    BottomSheetPages(navigator = pages, label = "editWordPages") { page ->
        when (page) {
            EditWordPage.Edit -> EditContent(
                originalWord = originalWord,
                onOriginalWordChange = { originalWord = it },
                translation = translation,
                onTranslationChange = { translation = it },
                description = description,
                onDescriptionChange = { description = it },
                isSaveEnabled = isSaveEnabled,
                onSave = {
                    onSave(
                        word.copy(
                            originalWord = originalWord.trim(),
                            translation = translation.trim(),
                            description = description.trim()
                        )
                    )
                    navigator.dismiss()
                },
                onDeleteRequest = {
                    focusManager.clearFocus()
                    pages.navigateTo(EditWordPage.ConfirmDelete)
                },
                focusManager = focusManager
            )

            EditWordPage.ConfirmDelete -> DeleteConfirmContent(
                wordName = word.originalWord,
                onConfirm = {
                    onDelete()
                    navigator.dismiss()
                },
                onCancel = { pages.navigateBack() }
            )
        }
    }
}

@Composable
private fun EditContent(
    originalWord: String,
    onOriginalWordChange: (String) -> Unit,
    translation: String,
    onTranslationChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isSaveEnabled: Boolean,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
    ) {
        Text(
            text = stringResource(Res.string.edit_word),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = originalWord,
            onValueChange = onOriginalWordChange,
            label = { Text(stringResource(Res.string.original_word)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(Theme.shapes.medium),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        OutlinedTextField(
            value = translation,
            onValueChange = onTranslationChange,
            label = { Text(stringResource(Res.string.translation_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(Theme.shapes.medium),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(Res.string.description_optional)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(Theme.shapes.medium),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(Modifier.height(Theme.spacing.xs))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight),
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(
                text = stringResource(Res.string.save),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = onDeleteRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(Res.string.delete),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmContent(
    wordName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Theme.dimensions.iconSizeXLarge)
        )

        Text(
            text = stringResource(Res.string.delete_word_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(Res.string.delete_word_message, wordName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Theme.spacing.xs))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(
                text = stringResource(Res.string.delete),
                style = MaterialTheme.typography.labelLarge
            )
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight),
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(
                text = stringResource(Res.string.cancel),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
