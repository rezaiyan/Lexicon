package components.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import theme.Theme

/**
 * Labelled text field: bold label above (optional " · optional" suffix), outlined input, helper / error text below.
 * [readOnly] renders a muted surface for values the user can see but not change.
 */
@Composable
fun SheetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    optionalSuffix: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant
    val container = if (readOnly) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
    val accessibleLabel = if (optionalSuffix != null) "$label · $optionalSuffix" else label
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs)) {
        // The visible label is announced as the field's name instead, so screen readers and UI tests
        // find one labelled text field rather than a loose label next to an unnamed edit box.
        Text(
            modifier = Modifier.clearAndSetSemantics {},
            text = buildAnnotatedString {
                append(label)
                if (optionalSuffix != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = muted)) {
                        append(" · $optionalSuffix")
                    }
                }
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = muted,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth().semantics { contentDescription = accessibleLabel },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            textStyle = if (readOnly) textStyle.copy(color = muted) else textStyle,
            placeholder = placeholder?.let { { Text(it, maxLines = 1) } },
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(Theme.shapes.large - Theme.spacing.xxxs),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (readOnly) outline else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = outline,
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                disabledContainerColor = container,
            ),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        )
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else muted,
            )
        }
    }
}

@Composable
fun SheetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Theme.dimensions.touchTarget)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Theme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
    }
}
