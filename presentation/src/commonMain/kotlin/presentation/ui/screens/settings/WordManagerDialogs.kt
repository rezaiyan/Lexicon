package presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.ButtonType
import presentation.ui.components.LanguageSelectionDialog
import theme.Theme
import utils.Language
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.batch_edit_languages
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.delete_words_message
import lexicon.resources.generated.resources.delete_words_title
import lexicon.resources.generated.resources.deleting
import lexicon.resources.generated.resources.deleting_words
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.edit_word
import lexicon.resources.generated.resources.original_language
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.please_wait
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.word_language
import lexicon.resources.generated.resources.translation_language_label
import lexicon.resources.generated.resources.translation_label
import lexicon.resources.generated.resources.update_languages
import lexicon.resources.generated.resources.updating_languages

@Composable
internal fun EditWordDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit
) {
    var originalWord by remember { mutableStateOf(word.originalWord) }
    var translation by remember { mutableStateOf(word.translation) }
    var description by remember { mutableStateOf(word.description) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Edit,
        title = stringResource(Res.string.edit_word),
        primaryButtonText = stringResource(Res.string.save),
        primaryButtonOnClick = {
            if (originalWord.isNotBlank() && translation.isNotBlank()) {
                val updatedWord = word.copy(
                    originalWord = originalWord.trim(),
                    translation = translation.trim(),
                    description = description.trim()
                )
                onSave(updatedWord)
            }
        },
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss,
        content = {
            Column(
                modifier = Modifier.padding(top = Theme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
            ) {
                OutlinedTextField(
                    value = originalWord,
                    onValueChange = { originalWord = it },
                    label = { Text(stringResource(Res.string.original_word)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text(stringResource(Res.string.translation_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}

@Composable
internal fun DeleteConfirmationDialog(
    isDeleting: Boolean,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        icon = if (isDeleting) null else Icons.Default.Warning,
        iconTint = if (isDeleting) null else MaterialTheme.colorScheme.error,
        title = if (isDeleting)
            stringResource(Res.string.deleting_words)
        else
            stringResource(Res.string.delete_words_title),
        primaryButtonText = if (isDeleting)
            stringResource(Res.string.deleting)
        else
            stringResource(Res.string.delete),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = if (isDeleting) null else stringResource(Res.string.cancel),
        secondaryButtonOnClick = if (isDeleting) null else onDismiss,
        content = {
            Column(
                modifier = Modifier.padding(top = Theme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.small))
                    Text(
                        "deleting_words_message, count",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
                        Text(
                            stringResource(Res.string.please_wait),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        stringResource(Res.string.delete_words_message, count),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
internal fun BatchEditLanguagesDialog(
    isUpdating: Boolean,
    count: Int,
    initialSourceLanguage: Language,
    initialTargetLanguage: Language,
    onConfirm: (sourceLanguage: Language, targetLanguage: Language) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSourceLanguage by remember { mutableStateOf(initialSourceLanguage) }
    var selectedTargetLanguage by remember { mutableStateOf(initialTargetLanguage) }
    var showSourceLanguagePicker by remember { mutableStateOf(false) }
    var showTargetLanguagePicker by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = {
            if (!isUpdating) onDismiss()
        },
        icon = if (isUpdating) null else Icons.Default.Language,
        title = if (isUpdating)
            stringResource(Res.string.updating_languages)
        else
            stringResource(Res.string.batch_edit_languages),
        primaryButtonText = if (isUpdating)
            stringResource(Res.string.updating_languages)
        else
            stringResource(Res.string.update_languages),
        primaryButtonOnClick = {
            if (!isUpdating) {
                onConfirm(selectedSourceLanguage, selectedTargetLanguage)
            }
        },
        secondaryButtonText = if (isUpdating) null else stringResource(Res.string.cancel),
        secondaryButtonOnClick = if (isUpdating) null else onDismiss,
        content = {
            Column(
                modifier = Modifier.padding(top = Theme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.small))
                    Text(
                        stringResource(Res.string.updating_languages),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
                    Text(
                        stringResource(Res.string.please_wait),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "Update $count word(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Word language selector (targetLanguage = language of originalWord)
                    LanguageSelectorCard(
                        label = stringResource(Res.string.word_language),
                        language = selectedTargetLanguage,
                        onClick = { showTargetLanguagePicker = true }
                    )

                    // Translation language selector (sourceLanguage = language of translation)
                    LanguageSelectorCard(
                        label = stringResource(Res.string.translation_language_label),
                        language = selectedSourceLanguage,
                        onClick = { showSourceLanguagePicker = true }
                    )
                }
            }
        }
    )

    // Language picker dialogs
    if (showSourceLanguagePicker) {
        LanguageSelectionDialog(
            currentLanguage = selectedSourceLanguage,
            onDismiss = { showSourceLanguagePicker = false },
            onLanguageSelected = { language ->
                selectedSourceLanguage = language
                showSourceLanguagePicker = false
            }
        )
    }

    if (showTargetLanguagePicker) {
        LanguageSelectionDialog(
            currentLanguage = selectedTargetLanguage,
            onDismiss = { showTargetLanguagePicker = false },
            onLanguageSelected = { language ->
                selectedTargetLanguage = language
                showTargetLanguagePicker = false
            }
        )
    }
}

@Composable
private fun LanguageSelectorCard(
    label: String,
    language: Language,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
