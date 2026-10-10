package feature.insights.coach.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.Pill
import feature.insights.coach.model.DeepDiveUi
import feature.insights.coach.model.SectionKind
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_best_hour
import lexicon.resources.generated.resources.insights_coach_best_score
import lexicon.resources.generated.resources.insights_coach_comebacks
import lexicon.resources.generated.resources.insights_coach_games_played
import lexicon.resources.generated.resources.insights_coach_hardest
import lexicon.resources.generated.resources.insights_coach_locked
import lexicon.resources.generated.resources.insights_coach_moved_up
import lexicon.resources.generated.resources.insights_coach_section_habits
import lexicon.resources.generated.resources.insights_coach_section_mastery
import lexicon.resources.generated.resources.insights_coach_section_word_rush
import lexicon.resources.generated.resources.insights_coach_section_words
import lexicon.resources.generated.resources.insights_coach_slipped
import lexicon.resources.generated.resources.insights_coach_stage
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val StageLabelWidth = 64.dp
private val WeekdayChartHeight = 96.dp
private val WeekdayBarWidth = 20.dp

/** Bar height in dp per accuracy percentage point (100% → 60dp, leaving room for labels). */
private const val BAR_DP_PER_PERCENT = 0.6f

/** Collapsible deep-dive shell; locked sections show a progress teaser instead of an empty chart. */
@Composable
internal fun DeepDiveSection(section: DeepDiveUi, expanded: Boolean, onToggle: (SectionKind) -> Unit) {
    val chevron by animateFloatAsState(if (expanded) 180f else 0f)
    val locked = section is DeepDiveUi.Locked
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = !locked) { onToggle(section.kind) }
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titleFor(section.kind), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (section.caption.isNotEmpty()) {
                    Text(
                        text = section.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = if (locked) Icons.Rounded.Lock else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = if (locked) Modifier else Modifier.rotate(chevron),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (section is DeepDiveUi.Locked) {
            LinearProgressIndicator(progress = { section.progress }, modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(Res.string.insights_coach_locked, section.reviewsNeeded),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        AnimatedVisibility(visible = expanded && !locked) {
            // Chart reads the caption aloud as its takeaway.
            Box(Modifier.semantics { contentDescription = section.caption }) {
                when (section) {
                    is DeepDiveUi.Mastery -> MasteryBody(section)
                    is DeepDiveUi.Habits -> HabitsBody(section)
                    is DeepDiveUi.Words -> WordsBody(section)
                    is DeepDiveUi.WordRush -> WordRushBody(section)
                    is DeepDiveUi.Locked -> Unit
                }
            }
        }
    }
}

@Composable
private fun titleFor(kind: SectionKind) = stringResource(
    when (kind) {
        SectionKind.MASTERY -> Res.string.insights_coach_section_mastery
        SectionKind.HABITS -> Res.string.insights_coach_section_habits
        SectionKind.WORDS -> Res.string.insights_coach_section_words
        SectionKind.WORD_RUSH -> Res.string.insights_coach_section_word_rush
    }
)

@Composable
private fun MasteryBody(section: DeepDiveUi.Mastery) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        section.levels.forEach { bar ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                Text(
                    text = stringResource(Res.string.insights_coach_stage, bar.level),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(StageLabelWidth),
                )
                LinearProgressIndicator(progress = { bar.fraction }, modifier = Modifier.weight(1f))
                Text(bar.words.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            Pill(
                text = stringResource(Res.string.insights_coach_moved_up, section.promoted),
                color = MaterialTheme.colorScheme.primary,
            )
            if (section.demoted > 0) {
                Pill(
                    text = stringResource(Res.string.insights_coach_slipped, section.demoted),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HabitsBody(section: DeepDiveUi.Habits) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        val hour = section.bestHourLabel
        val accuracy = section.bestHourAccuracy
        if (hour != null && accuracy != null) {
            Text(
                text = stringResource(Res.string.insights_coach_best_hour, hour, accuracy),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(WeekdayChartHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            section.weekdays.forEach { day ->
                val barColor = if (day.isBest) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.disabled)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${day.accuracyPct}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.isBest) FontWeight.Bold else FontWeight.Normal,
                    )
                    Box(
                        Modifier
                            .width(WeekdayBarWidth)
                            .height((day.accuracyPct * BAR_DP_PER_PERCENT).dp)
                            .clip(RoundedCornerShape(Theme.shapes.small))
                            .background(barColor)
                    )
                    Text(weekdayInitial(day.isoDay), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun WordsBody(section: DeepDiveUi.Words) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        if (section.hardest.isNotEmpty()) {
            Text(stringResource(Res.string.insights_coach_hardest), style = MaterialTheme.typography.labelLarge)
            section.hardest.forEach { word ->
                Row {
                    Text(word.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        text = "${word.accuracyPct}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (section.comebacks.isNotEmpty()) {
            Text(stringResource(Res.string.insights_coach_comebacks), style = MaterialTheme.typography.labelLarge)
            section.comebacks.forEach { Text(it.text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun WordRushBody(section: DeepDiveUi.WordRush) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.lg)) {
        Column {
            Text(section.gamesPlayed, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.insights_coach_games_played), style = MaterialTheme.typography.labelMedium)
        }
        Column {
            Text(section.bestScore, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.insights_coach_best_score), style = MaterialTheme.typography.labelMedium)
        }
    }
}
