package feature.study.ui.review

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import components.sheet.ConfirmSheetContent
import components.sheet.ConfirmTone
import components.sheet.WordFormSheetPage
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.delete_word_message
import lexicon.resources.generated.resources.delete_word_title
import lexicon.resources.generated.resources.edit_word
import org.jetbrains.compose.resources.stringResource
import overlay.OverlayNavigator
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator

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

    BottomSheetPages(navigator = pages, onClose = { navigator.dismiss() }, label = "editWordPages") { page ->
        when (page) {
            EditWordPage.Edit -> WordFormSheetPage(
                title = stringResource(Res.string.edit_word),
                word = originalWord,
                onWordChange = { originalWord = it },
                translation = translation,
                onTranslationChange = { translation = it },
                description = description,
                onDescriptionChange = { description = it },
                saveEnabled = isSaveEnabled,
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
                onCancel = { navigator.dismiss() },
                onDelete = {
                    focusManager.clearFocus()
                    pages.navigateTo(EditWordPage.ConfirmDelete)
                },
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
private fun DeleteConfirmContent(
    wordName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    ConfirmSheetContent(
        icon = Icons.Default.DeleteOutline,
        title = stringResource(Res.string.delete_word_title),
        message = stringResource(Res.string.delete_word_message, wordName),
        confirmText = stringResource(Res.string.delete),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onCancel,
        tone = ConfirmTone.Danger,
    )
}
