package presentation.ui.components.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.sheet.ConfirmDialog
import components.sheet.ConfirmTone
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTonalButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_words_duplicates_skipped
import lexicon.resources.generated.resources.add_more_words
import lexicon.resources.generated.resources.ai_wizard_discard
import lexicon.resources.generated.resources.ai_wizard_discard_message
import lexicon.resources.generated.resources.ai_wizard_discard_title
import lexicon.resources.generated.resources.done
import lexicon.resources.generated.resources.keep_editing
import lexicon.resources.generated.resources.start_review
import lexicon.resources.generated.resources.word_added_title
import lexicon.resources.generated.resources.words_added_subtitle
import lexicon.resources.generated.resources.words_added_title
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private const val MaxPreviewWords = 3

/**
 * "Discard these words?" — shown when leaving a flow that holds unsaved suggestions.
 * A real Dialog (not a nested sheet) so it can be raised from inside any sheet page.
 */
@Composable
internal fun DiscardConfirmationDialog(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    ConfirmDialog(
        icon = Icons.Default.DeleteOutline,
        title = stringResource(Res.string.ai_wizard_discard_title),
        message = stringResource(Res.string.ai_wizard_discard_message),
        confirmText = stringResource(Res.string.ai_wizard_discard),
        onConfirm = onDiscard,
        dismissText = stringResource(Res.string.keep_editing),
        onDismiss = onKeep,
        tone = ConfirmTone.Danger,
    )
}

/** "18 words added" — closing page of every flow, with a shortcut into review. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImportSuccessContent(
    count: Int,
    previewWords: List<String>,
    onStartReview: (() -> Unit)?,
    onAddMore: () -> Unit,
    onDone: () -> Unit,
    duplicates: Int = 0,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = Theme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSizeMassive + Theme.spacing.lg - Theme.spacing.xxs)
                .clip(CircleShape)
                .background(AppColors.secondary.copy(alpha = Theme.opacity.focus + Theme.opacity.hover)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = AppColors.secondary,
                modifier = Modifier.size(Theme.dimensions.iconSizeHuge - Theme.spacing.xs),
            )
        }

        Column(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
        ) {
            Text(
                text = if (count == 1) stringResource(Res.string.word_added_title)
                else stringResource(Res.string.words_added_title, count),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.words_added_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (duplicates > 0) {
                Text(
                    stringResource(Res.string.add_words_duplicates_skipped, duplicates),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (previewWords.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    Theme.spacing.xxs + Theme.spacing.xxxs,
                    Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
            ) {
                previewWords.take(MaxPreviewWords).forEach { PreviewChip(it) }
                val more = count - MaxPreviewWords
                if (more > 0) PreviewChip("+$more", muted = true)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            if (onStartReview != null) {
                SheetPrimaryButton(text = stringResource(Res.string.start_review), onClick = onStartReview)
                SheetTonalButton(text = stringResource(Res.string.add_more_words), onClick = onAddMore)
            } else {
                SheetPrimaryButton(text = stringResource(Res.string.done), onClick = onDone)
                SheetTonalButton(text = stringResource(Res.string.add_more_words), onClick = onAddMore)
            }
        }
    }
}

@Composable
private fun PreviewChip(text: String, muted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(Theme.shapes.small + Theme.spacing.xxxs))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs + Theme.spacing.xxxs),
    )
}
