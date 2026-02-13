@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.screens.review

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.ButtonType
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.delete_words_title

@Composable
fun DeleteWordConfirmationDialog(
    word: Word,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.error,
        title = stringResource(Res.string.delete_words_title),
        message = "Are you sure you want to delete \"${word.originalWord}\"? This action cannot be undone.",
        primaryButtonText = stringResource(Res.string.delete),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss
    )
}

