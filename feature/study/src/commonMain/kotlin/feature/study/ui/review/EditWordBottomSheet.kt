package feature.study.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
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
    LexiconDialogContent(
        title = stringResource(Res.string.edit_word),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
            ) {
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
            }
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.save),
            onClick = onSave,
            enabled = isSaveEnabled
        ),
        negativeButton = ButtonState(
            text = stringResource(Res.string.delete),
            onClick = onDeleteRequest,
            type = ButtonType.Error
        )
    )
}

@Composable
private fun DeleteConfirmContent(
    wordName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    LexiconDialogContent(
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.error,
        title = stringResource(Res.string.delete_word_title),
        message = stringResource(Res.string.delete_word_message, wordName),
        primaryButtonText = stringResource(Res.string.delete),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onCancel
    )
}
