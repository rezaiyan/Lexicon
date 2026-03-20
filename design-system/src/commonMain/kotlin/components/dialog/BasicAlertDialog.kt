package components.dialog

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import theme.Theme
import androidx.compose.material3.BasicAlertDialog as MaterialBasicAlertDialog

@Composable
fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    iconState: DialogIconState = DialogIconState.None,
    title: String? = null,
    message: String? = null,
    progressState: DialogProgressState = DialogProgressState.None,
    content: @Composable (() -> Unit)? = null,
    primaryButton: ButtonState? = null,
    secondaryButton: ButtonState? = null,
    negativeButton: ButtonState? = null
) {
    MaterialBasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints {
            val hasBoundedHeight = constraints.hasBoundedHeight
            val maxDialogHeight = if (hasBoundedHeight) maxHeight * 0.85f else 560.dp
            val shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)

            Surface(
                shape = shape,
                shadowElevation = Theme.elevation.overlay,
                tonalElevation = Theme.elevation.medium,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(min = Theme.dimensions.dialogMinWidth, max = Theme.dimensions.dialogMaxWidth)
                    .heightIn(max = maxDialogHeight)
                    .clip(shape)
            ) {
                Column(
                    Modifier
                        .padding(Theme.spacing.lg)
                        .verticalScroll(rememberScrollState())
                ) {
                    LexiconDialogContent(
                        iconState = iconState,
                        title = title,
                        message = message,
                        progressState = progressState,
                        content = content,
                        primaryButton = primaryButton,
                        secondaryButton = secondaryButton,
                        negativeButton = negativeButton
                    )
                }
            }
        }
    }
}

@Composable
fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    message: String? = null,
    content: @Composable (() -> Unit)? = null,
    primaryButtonText: String? = null,
    primaryButtonOnClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    secondaryButtonOnClick: (() -> Unit)? = null,
    negativeButtonText: String? = null,
    negativeButtonOnClick: (() -> Unit)? = null,
    primaryButtonType: ButtonType = ButtonType.Default,
    iconTint: Color? = null
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        iconState = when {
            icon != null -> DialogIconState.Icon(icon, iconTint)
            else -> DialogIconState.None
        },
        title = title,
        message = message,
        progressState = DialogProgressState.None,
        content = content,
        primaryButton = primaryButtonText?.let { text ->
            primaryButtonOnClick?.let { onClick ->
                ButtonState(text = text, onClick = onClick, enabled = true, type = primaryButtonType)
            }
        },
        secondaryButton = secondaryButtonText?.let { text ->
            secondaryButtonOnClick?.let { onClick ->
                ButtonState(text = text, onClick = onClick, enabled = true, type = ButtonType.Default)
            }
        },
        negativeButton = negativeButtonText?.let { text ->
            negativeButtonOnClick?.let { onClick ->
                ButtonState(text = text, onClick = onClick, enabled = true, type = ButtonType.Error)
            }
        }
    )
}
