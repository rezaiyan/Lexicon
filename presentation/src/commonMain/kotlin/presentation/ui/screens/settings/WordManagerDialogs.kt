package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.batch_edit_languages
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.delete_words_message
import lexicon.resources.generated.resources.delete_words_title
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.edit_word
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.translation_language_label
import lexicon.resources.generated.resources.update_languages
import lexicon.resources.generated.resources.update_words_count
import lexicon.resources.generated.resources.word_language
import org.jetbrains.compose.resources.stringResource
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import overlay.LocalOverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.LanguageSelectionContent
import theme.Theme
import utils.Language
import lexicon.resources.generated.resources.translation_label

@Composable
internal fun EditWordContent(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit
) {
    var originalWord by remember { mutableStateOf(word.originalWord) }
    var translation by remember { mutableStateOf(word.translation) }
    var description by remember { mutableStateOf(word.description) }
    val focusManager = LocalFocusManager.current

    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg)
            .imePadding(),
        iconState = components.dialog.DialogIconState.Icon(Icons.Default.Edit),
        title = stringResource(Res.string.edit_word),
            content = {
                Column(
                    modifier = Modifier.padding(top = Theme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
                ) {
                    OutlinedTextField(
                        value = originalWord,
                        onValueChange = { originalWord = it },
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
                        onValueChange = { translation = it },
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
                        onValueChange = { description = it },
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
                onClick = {
                    if (originalWord.isNotBlank() && translation.isNotBlank()) {
                        onSave(
                            word.copy(
                                originalWord = originalWord.trim(),
                                translation = translation.trim(),
                                description = description.trim()
                            )
                        )
                    }
                }
            ),
            secondaryButton = ButtonState(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss
            )
        )
}

@Composable
internal fun DeleteConfirmationContent(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        iconState = components.dialog.DialogIconState.Icon(
            imageVector = Icons.Default.Warning,
            tint = MaterialTheme.colorScheme.error
        ),
        title = stringResource(Res.string.delete_words_title),
        content = {
            Text(
                stringResource(Res.string.delete_words_message, count),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.delete),
            onClick = onConfirm,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
internal fun BatchEditLanguagesContent(
    count: Int,
    initialSourceLanguage: Language,
    initialTargetLanguage: Language,
    onConfirm: (sourceLanguage: Language, targetLanguage: Language) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSourceLanguage by remember { mutableStateOf(initialSourceLanguage) }
    var selectedTargetLanguage by remember { mutableStateOf(initialTargetLanguage) }
    val overlayHost = LocalOverlayHost.current

    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        iconState = components.dialog.DialogIconState.Icon(Icons.Default.Language),
        title = stringResource(Res.string.batch_edit_languages),
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
                ) {
                    Text(
                        stringResource(Res.string.update_words_count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LanguageSelectorCard(
                        label = stringResource(Res.string.word_language),
                        language = selectedTargetLanguage,
                        onClick = {
                            overlayHost.showSizeToFitBottomSheet(tag = "target-language") { nav ->
                                LanguageSelectionContent(
                                    currentLanguage = selectedTargetLanguage,
                                    onLanguageSelected = { language ->
                                        selectedTargetLanguage = language
                                        nav.dismiss()
                                    }
                                )
                            }
                        }
                    )

                    LanguageSelectorCard(
                        label = stringResource(Res.string.translation_language_label),
                        language = selectedSourceLanguage,
                        onClick = {
                            overlayHost.showSizeToFitBottomSheet(tag = "source-language") { nav ->
                                LanguageSelectionContent(
                                    currentLanguage = selectedSourceLanguage,
                                    onLanguageSelected = { language ->
                                        selectedSourceLanguage = language
                                        nav.dismiss()
                                    }
                                )
                            }
                        }
                    )
                }
            },
            primaryButton = ButtonState(
                text = stringResource(Res.string.update_languages),
                onClick = { onConfirm(selectedSourceLanguage, selectedTargetLanguage) }
            ),
            secondaryButton = ButtonState(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss
            )
        )
}

@Composable
private fun LanguageSelectorCard(
    label: String,
    language: Language,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${language.nativeName} (${language.displayName})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
