package feature.support.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.support_chars_left
import lexicon.resources.generated.resources.support_input_placeholder
import lexicon.resources.generated.resources.support_send
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Message input pinned above the keyboard: a growing pill field and a send button that lights up. */
@Composable
internal fun Composer(
    draft: String,
    canSend: Boolean,
    remainingChars: Int?,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(thickness = Theme.dimensions.hairlineThickness, color = colors.outlineVariant)
        AnimatedVisibility(
            visible = remainingChars != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = stringResource(Res.string.support_chars_left, remainingChars ?: 0),
                style = MaterialTheme.typography.labelSmall,
                color = if ((remainingChars ?: 0) <= 0) colors.error else colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Theme.spacing.xxs, end = Theme.spacing.screenGutter),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Theme.spacing.screenGutter,
                    end = Theme.spacing.xs,
                    top = Theme.spacing.xs,
                    bottom = Theme.spacing.xs,
                ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            DraftField(draft = draft, onDraftChanged = onDraftChanged, modifier = Modifier.weight(1f))
            SendButton(enabled = canSend, onClick = onSend)
        }
    }
}

@Composable
private fun DraftField(draft: String, onDraftChanged: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    BasicTextField(
        value = draft,
        onValueChange = onDraftChanged,
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        maxLines = COMPOSER_MAX_LINES,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { field ->
            Box(
                modifier = Modifier
                    .heightIn(min = Theme.dimensions.touchTarget)
                    .clip(RoundedCornerShape(Theme.shapes.extraLarge))
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.support_input_placeholder),
                        style = textStyle,
                        color = colors.onSurfaceVariant,
                    )
                }
                field()
            }
        },
    )
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = Theme.motion
    val spec = tween<Float>(motion.durationShort2, easing = motion.easingStandard)
    val colorSpec = tween<Color>(motion.durationShort2, easing = motion.easingStandard)
    val container by animateColorAsState(
        targetValue = if (enabled) colors.primary else colors.surfaceContainerHigh,
        animationSpec = colorSpec,
    )
    val content by animateColorAsState(
        targetValue = if (enabled) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = colorSpec,
    )
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else DISABLED_SEND_SCALE,
        animationSpec = spec,
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(Theme.dimensions.touchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(Res.string.support_send),
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
    }
}

private const val COMPOSER_MAX_LINES = 6
private const val DISABLED_SEND_SCALE = 0.9f
