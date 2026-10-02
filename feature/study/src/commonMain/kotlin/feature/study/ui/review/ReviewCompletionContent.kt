package feature.study.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.animation.ConfettiOverlay
import components.animation.staggeredFadeSlide
import components.sheet.SheetGroup
import components.sheet.SheetSectionLabel
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.completion_good_message
import lexicon.resources.generated.resources.completion_good_title
import lexicon.resources.generated.resources.completion_great_message
import lexicon.resources.generated.resources.completion_great_title
import lexicon.resources.generated.resources.completion_missed_title
import lexicon.resources.generated.resources.completion_okay_message
import lexicon.resources.generated.resources.completion_okay_title
import lexicon.resources.generated.resources.completion_perfect_message
import lexicon.resources.generated.resources.completion_perfect_title
import lexicon.resources.generated.resources.completion_streak_days
import lexicon.resources.generated.resources.completion_tough_message
import lexicon.resources.generated.resources.completion_tough_title
import lexicon.resources.generated.resources.done
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

/**
 * Review session summary: score ring, tier title and message, streak, remembered/forgot split and the
 * words to revisit, scrolling above a pinned Done footer. Confetti plays behind, scaled to the score.
 */
@Composable
fun ReviewCompletionContent(
    knownCount: Int,
    unknownCount: Int,
    missedWords: List<Word>,
    newStreak: Int?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalCount = knownCount + unknownCount
    val scorePercent = if (totalCount > 0) (knownCount * 100) / totalCount else 0
    val tier = remember(scorePercent) { PerformanceTier.fromScore(scorePercent) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        if (knownCount > 0) {
            ConfettiOverlay(
                particleCount = when {
                    scorePercent == 100 -> 140
                    scorePercent >= 80 -> 100
                    scorePercent >= 60 -> 70
                    else -> 50
                },
                durationMs = 4000,
            )
        }

        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Theme.spacing.md)
                    .padding(top = Theme.spacing.xxl, bottom = Theme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
            ) {
                ScoreRing(
                    scorePercent = scorePercent,
                    gradientColors = tier.gradientColors,
                    modifier = Modifier.staggeredFadeSlide(index = 0),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    modifier = Modifier.staggeredFadeSlide(index = 1),
                ) {
                    Text(
                        text = stringResource(tier.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(tier.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (newStreak != null && newStreak > 0) {
                        StreakBadge(streakDays = newStreak, modifier = Modifier.padding(top = Theme.spacing.xxs))
                    }
                }

                StatsSection(
                    knownCount = knownCount,
                    unknownCount = unknownCount,
                    modifier = Modifier.staggeredFadeSlide(index = 2),
                )

                if (missedWords.isNotEmpty()) {
                    MissedWordsSection(
                        words = missedWords,
                        modifier = Modifier.staggeredFadeSlide(index = 3),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm)
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(Theme.shapes.pill),
            ) {
                Text(
                    text = stringResource(Res.string.done),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─── Performance Tiers ──────────────────────────────────────────────────────

internal enum class PerformanceTier(
    val titleRes: org.jetbrains.compose.resources.StringResource,
    val messageRes: org.jetbrains.compose.resources.StringResource,
    val gradientColors: List<Color>,
) {
    PERFECT(
        titleRes = Res.string.completion_perfect_title,
        messageRes = Res.string.completion_perfect_message,
        gradientColors = listOf(AppColors.secondary, AppColors.accentEmerald),
    ),
    GREAT(
        titleRes = Res.string.completion_great_title,
        messageRes = Res.string.completion_great_message,
        gradientColors = listOf(AppColors.primary, AppColors.accentLavender),
    ),
    GOOD(
        titleRes = Res.string.completion_good_title,
        messageRes = Res.string.completion_good_message,
        gradientColors = listOf(AppColors.primary, AppColors.accentSkyBlue),
    ),
    OKAY(
        titleRes = Res.string.completion_okay_title,
        messageRes = Res.string.completion_okay_message,
        gradientColors = listOf(AppColors.tertiary, AppColors.accentAmber),
    ),
    TOUGH(
        titleRes = Res.string.completion_tough_title,
        messageRes = Res.string.completion_tough_message,
        gradientColors = listOf(AppColors.tertiary, AppColors.error),
    );

    companion object {
        fun fromScore(percent: Int): PerformanceTier = when {
            percent == 100 -> PERFECT
            percent >= 80 -> GREAT
            percent >= 60 -> GOOD
            percent >= 40 -> OKAY
            else -> TOUGH
        }
    }
}

// ─── Streak Badge ────────────────────────────────────────────────────────────

@Composable
private fun StreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(AppColors.tertiary.copy(alpha = Theme.opacity.focus))
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = AppColors.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(Res.string.completion_streak_days, streakDays),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.tertiary,
        )
    }
}

// ─── Missed Words Section ────────────────────────────────────────────────────

@Composable
private fun MissedWordsSection(
    words: List<Word>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        SheetSectionLabel(stringResource(Res.string.completion_missed_title))
        SheetGroup {
            words.forEachIndexed { index, word ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
                ) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (index < words.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
                        modifier = Modifier.padding(start = Theme.spacing.md),
                    )
                }
            }
        }
    }
}
