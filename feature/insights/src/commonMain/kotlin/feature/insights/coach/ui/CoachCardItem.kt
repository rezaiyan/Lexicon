package feature.insights.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.AccentCard
import components.AccentIconBadge
import components.Pill
import domain.insights.model.CoachAction
import feature.insights.coach.model.CoachCardUi
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_dismiss
import lexicon.resources.generated.resources.insights_coach_more_words
import lexicon.resources.generated.resources.insights_coach_reminder_at
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Generic coach card: renders any server type from title/body/words/action. */
@Composable
internal fun CoachCardItem(
    card: CoachCardUi,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    AccentCard(
        accent = accent,
        enabled = true,
        onClick = { onAction(card) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Theme.spacing.md), verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                AccentIconBadge(icon = iconFor(card.type), accent = accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = card.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDismiss(card) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.insights_coach_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (card.words.isNotEmpty()) WordChips(card)
            ActionButton(card, onAction)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordChips(card: CoachCardUi) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        card.words.forEach { word ->
            Pill(text = word.text, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
        if (card.moreWordsCount > 0) {
            Pill(
                text = stringResource(Res.string.insights_coach_more_words, card.moreWordsCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionButton(card: CoachCardUi, onAction: (CoachCardUi) -> Unit) {
    val label = when (val action = card.action) {
        is CoachAction.ReviewWords -> action.label
        is CoachAction.StartReview -> action.label
        is CoachAction.StartWordRush -> action.label
        is CoachAction.EnableReminder -> {
            val hour = card.hourLabel
            if (hour != null) {
                "${action.label} · ${stringResource(Res.string.insights_coach_reminder_at, hour)}"
            } else {
                action.label
            }
        }
        CoachAction.None -> return
    }
    FilledTonalButton(onClick = { onAction(card) }) { Text(label) }
}

private fun iconFor(type: String): ImageVector = when (type) {
    "STREAK_AT_RISK" -> Icons.Rounded.LocalFireDepartment
    "SLIPPING_WORDS", "DIFFICULT_WORDS", "LEVEL_BOTTLENECK" -> Icons.Rounded.Refresh
    "BEST_TIME" -> Icons.Rounded.AlarmOn
    "MILESTONE_NEAR", "COMEBACK_WIN" -> Icons.Rounded.EmojiEvents
    "WEEK_TREND" -> Icons.Rounded.Bolt
    else -> Icons.Rounded.AutoAwesome
}
