package feature.insights.coach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import components.Pill
import feature.insights.coach.model.DayDotUi
import feature.insights.coach.model.HeroUi
import feature.insights.coach.model.StatUi
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_accuracy
import lexicon.resources.generated.resources.insights_coach_accuracy_locked
import lexicon.resources.generated.resources.insights_coach_leveled_up
import lexicon.resources.generated.resources.insights_coach_reviews
import lexicon.resources.generated.resources.insights_coach_streak
import lexicon.resources.generated.resources.insights_coach_this_week
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** This-week hero: headline, three big numbers with their change, and the week's activity dots. */
@Composable
internal fun HeroSection(hero: HeroUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(Theme.gradients.primaryWash)
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.insights_coach_this_week).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (hero.streak > 0) {
                Pill(
                    text = stringResource(Res.string.insights_coach_streak, hero.streak),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = hero.headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            HeroStat(stringResource(Res.string.insights_coach_reviews), hero.reviews, Modifier.weight(1f))
            val accuracy = hero.accuracy
            if (accuracy != null) {
                HeroStat(stringResource(Res.string.insights_coach_accuracy), accuracy, Modifier.weight(1f))
            } else {
                LockedStat(stringResource(Res.string.insights_coach_accuracy), Modifier.weight(1f))
            }
            HeroStat(stringResource(Res.string.insights_coach_leveled_up), hero.leveledUp, Modifier.weight(1f))
        }
        WeekDots(hero.week)
    }
}

@Composable
private fun HeroStat(label: String, stat: StatUi, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
        Text(
            text = stat.value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChangeLabel(stat)
    }
}

@Composable
private fun LockedStat(label: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
        Text(
            text = "—",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(Res.string.insights_coach_accuracy_locked),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekDots(week: List<DayDotUi>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { day -> DayDot(day) }
    }
}

@Composable
private fun DayDot(day: DayDotUi) {
    val label = weekdayInitial(day.isoDay)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
        modifier = Modifier.semantics { contentDescription = "$label ${day.reviews}" },
    ) {
        val fill = when {
            day.reviews > 0 -> MaterialTheme.colorScheme.primary
            day.isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Theme.opacity.overlay)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        Box(
            Modifier
                .size(Theme.dimensions.iconSize)
                .clip(CircleShape)
                .background(fill)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
