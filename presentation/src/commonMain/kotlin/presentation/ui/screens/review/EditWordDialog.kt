@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.BasicAlertDialog
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.edit_word
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.translation_label

@Composable
fun EditWordDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit,
    onDelete: () -> Unit
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
        negativeButtonText = stringResource(Res.string.delete),
        negativeButtonOnClick = onDelete,
        content = {
            Column(
                modifier = Modifier
                    .padding(top = Theme.spacing.small)
                    .verticalScroll(rememberScrollState()),
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

