package presentation.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import components.sheet.ConfirmSheetContent
import components.sheet.ConfirmTone
import components.sheet.SheetFooterRow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTonalButton
import components.sheet.WordFormSheetPage
import domain.tag.model.Tag
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.assign_tags_to_words_count
import lexicon.resources.generated.resources.batch_edit_languages
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.delete_words_message
import lexicon.resources.generated.resources.delete_words_title
import lexicon.resources.generated.resources.edit_word
import lexicon.resources.generated.resources.set_tag
import lexicon.resources.generated.resources.translation_language_label
import lexicon.resources.generated.resources.update_languages
import lexicon.resources.generated.resources.update_words_count
import lexicon.resources.generated.resources.word_language
import org.jetbrains.compose.resources.stringResource
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import presentation.ui.components.LanguageSelectionContent
import presentation.ui.components.imports.LanguagePairCard
import utils.Language

@Composable
internal fun EditWordContent(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit
) {
    var originalWord by remember { mutableStateOf(word.originalWord) }
    var translation by remember { mutableStateOf(word.translation) }
    var description by remember { mutableStateOf(word.description) }

    WordFormSheetPage(
        title = stringResource(Res.string.edit_word),
        word = originalWord,
        onWordChange = { originalWord = it },
        translation = translation,
        onTranslationChange = { translation = it },
        description = description,
        onDescriptionChange = { description = it },
        onSave = {
            onSave(
                word.copy(
                    originalWord = originalWord.trim(),
                    translation = translation.trim(),
                    description = description.trim()
                )
            )
        },
        onCancel = onDismiss,
    )
}

@Composable
internal fun DeleteConfirmationContent(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    ConfirmSheetContent(
        icon = Icons.Default.DeleteOutline,
        title = stringResource(Res.string.delete_words_title),
        message = stringResource(Res.string.delete_words_message, count),
        confirmText = stringResource(Res.string.delete),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        tone = ConfirmTone.Danger,
        onClose = onClose,
    )
}

private sealed interface BatchLanguagesPage {
    data object Form : BatchLanguagesPage
    data object PickTarget : BatchLanguagesPage
    data object PickSource : BatchLanguagesPage
}

/** Batch language edit. Pickers open as pages inside the same sheet. */
@Composable
internal fun BatchEditLanguagesContent(
    count: Int,
    initialSourceLanguage: Language,
    initialTargetLanguage: Language,
    onConfirm: (sourceLanguage: Language, targetLanguage: Language) -> Unit,
    onDismiss: () -> Unit
) {
    var source by remember { mutableStateOf(initialSourceLanguage) }
    var target by remember { mutableStateOf(initialTargetLanguage) }
    val pages = rememberBottomSheetPageNavigator<BatchLanguagesPage>(BatchLanguagesPage.Form)
    val targetLabel = stringResource(Res.string.word_language)
    val sourceLabel = stringResource(Res.string.translation_language_label)

    BottomSheetPages(navigator = pages, onClose = onDismiss, label = "batchLanguagesPages") { page ->
        when (page) {
            BatchLanguagesPage.Form -> SheetPage(
                title = stringResource(Res.string.batch_edit_languages),
                subtitle = stringResource(Res.string.update_words_count, count),
                footer = {
                    SheetFooterRow(
                        secondary = {
                            SheetTonalButton(
                                text = stringResource(Res.string.cancel),
                                onClick = onDismiss,
                                modifier = it,
                            )
                        },
                        primary = {
                            SheetPrimaryButton(
                                text = stringResource(Res.string.update_languages),
                                onClick = { onConfirm(source, target) },
                                modifier = it,
                            )
                        },
                    )
                },
            ) {
                LanguagePairCard(
                    topLabel = targetLabel,
                    top = target,
                    onTopClick = { pages.navigateTo(BatchLanguagesPage.PickTarget) },
                    bottomLabel = sourceLabel,
                    bottom = source,
                    onBottomClick = { pages.navigateTo(BatchLanguagesPage.PickSource) },
                    onSwap = {
                        val previousTarget = target
                        target = source
                        source = previousTarget
                    },
                )
            }

            BatchLanguagesPage.PickTarget -> LanguageSelectionContent(
                currentLanguage = target,
                onLanguageSelected = {
                    target = it
                    pages.navigateBack()
                },
                title = targetLabel,
            )

            BatchLanguagesPage.PickSource -> LanguageSelectionContent(
                currentLanguage = source,
                onLanguageSelected = {
                    source = it
                    pages.navigateBack()
                },
                title = sourceLabel,
            )
        }
    }
}

@Composable
internal fun BatchTagAssignmentContent(
    count: Int,
    tags: List<Tag>,
    onConfirm: (tagIds: List<Long>) -> Unit,
    onClose: (() -> Unit)? = null,
) {
    var selectedTagIds by remember { mutableStateOf(emptySet<Long>()) }

    TagChecklistContent(
        title = stringResource(Res.string.set_tag),
        subtitle = stringResource(Res.string.assign_tags_to_words_count, count),
        tags = tags,
        selectedTagIds = selectedTagIds,
        onToggle = { id -> selectedTagIds = if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id },
        onApply = { onConfirm(selectedTagIds.toList()) },
        onClose = onClose,
    )
}
