package components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete_word
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.translation_label
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Word / translation / description form shared by every "edit word" sheet
 * (word manager, review, photo extraction review). State is hoisted to the caller.
 * [onDelete] adds a destructive text action below the fields; [extraContent] goes between the two.
 */
@Composable
fun WordFormSheetPage(
    title: String,
    word: String,
    onWordChange: (String) -> Unit,
    translation: String,
    onTranslationChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = word.isNotBlank() && translation.isNotBlank(),
    onDelete: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val translationFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }

    SheetPage(
        title = title,
        modifier = modifier,
        onBack = onBack,
        onClose = onClose,
        footer = {
            SheetFooterRow(
                secondary = {
                    SheetTonalButton(text = stringResource(Res.string.cancel), onClick = onCancel, modifier = it)
                },
                primary = {
                    SheetPrimaryButton(
                        text = stringResource(Res.string.save),
                        onClick = onSave,
                        enabled = saveEnabled,
                        modifier = it,
                    )
                },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            SheetField(
                label = stringResource(Res.string.original_word),
                value = word,
                onValueChange = onWordChange,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                imeAction = ImeAction.Next,
                onImeAction = { translationFocus.requestFocus() },
            )
            SheetField(
                label = stringResource(Res.string.translation_label),
                value = translation,
                onValueChange = onTranslationChange,
                imeAction = ImeAction.Next,
                onImeAction = { descriptionFocus.requestFocus() },
                modifier = Modifier.focusRequester(translationFocus),
            )
            SheetField(
                label = stringResource(Res.string.description_optional),
                value = description,
                onValueChange = onDescriptionChange,
                singleLine = false,
                minLines = 2,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                modifier = Modifier.focusRequester(descriptionFocus),
            )
        }
        extraContent?.invoke(this)
        onDelete?.let {
            SheetTextButton(
                text = stringResource(Res.string.delete_word),
                onClick = it,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
