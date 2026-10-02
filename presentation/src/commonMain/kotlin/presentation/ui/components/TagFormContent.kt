package presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import components.sheet.SheetField
import components.sheet.SheetFooterRow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTonalButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.tag_name_hint
import org.jetbrains.compose.resources.stringResource

/**
 * Create / rename a tag. Confirm is enabled once the trimmed name is non-blank and differs from [initialName].
 * Standalone sheets pass [onClose]; pager pages leave it null.
 */
@Composable
fun TagFormContent(
    title: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    onClose: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val trimmed = name.trim()
    val canConfirm = trimmed.isNotEmpty() && trimmed != initialName

    SheetPage(
        title = title,
        onClose = onClose,
        footer = {
            SheetFooterRow(
                secondary = {
                    SheetTonalButton(text = stringResource(Res.string.cancel), onClick = onDismiss, modifier = it)
                },
                primary = {
                    SheetPrimaryButton(
                        text = confirmText,
                        onClick = { onConfirm(trimmed) },
                        enabled = canConfirm,
                        modifier = it,
                    )
                },
            )
        },
    ) {
        SheetField(
            label = stringResource(Res.string.tag_name_hint),
            value = name,
            onValueChange = { name = it },
            imeAction = ImeAction.Done,
            onImeAction = { if (canConfirm) onConfirm(trimmed) },
        )
    }
}
