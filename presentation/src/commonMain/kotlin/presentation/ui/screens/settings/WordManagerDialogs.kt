package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.ButtonType
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.delete
import vokab.resources.generated.resources.delete_words_message
import vokab.resources.generated.resources.delete_words_title
import vokab.resources.generated.resources.deleting
import vokab.resources.generated.resources.deleting_words
import vokab.resources.generated.resources.description_optional
import vokab.resources.generated.resources.edit_word
import vokab.resources.generated.resources.original_word
import vokab.resources.generated.resources.please_wait
import vokab.resources.generated.resources.save
import vokab.resources.generated.resources.translation_label

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
