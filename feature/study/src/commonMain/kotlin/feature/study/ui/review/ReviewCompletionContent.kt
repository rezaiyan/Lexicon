package feature.study.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import components.animation.ConfettiOverlay
import components.animation.staggeredFadeSlide
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
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

/**
 * Review session completion screen — Airbnb-inspired modern design
 * with gamification psychology.
 *
 * Design principles applied:
 * - **Generous whitespace**: Spacious, breathable layout
 * - **Gradient score ring**: Premium feel with brand gradient fill
 * - **Card-based stats**: Elevated stat cards with color accents
 * - **Proportional bar**: Visual ratio of known vs unknown
 * - **Celebration**: Full-screen confetti burst scaled to performance
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
    val hasAnyKnown = knownCount > 0

    val tier = remember(scorePercent) { PerformanceTier.fromScore(scorePercent) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        // Confetti layer — behind content, scaled to performance
        if (hasAnyKnown) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Theme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(Theme.spacing.xxxl))

            // ── Gradient score ring ─────────────────────────────────────
            ScoreRing(
                scorePercent = scorePercent,
                gradientColors = tier.gradientColors,
                modifier = Modifier.staggeredFadeSlide(index = 0),
            )

            Spacer(Modifier.height(Theme.spacing.lg))

            // ── Performance title ───────────────────────────────────────
            Text(
                text = stringResource(tier.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .staggeredFadeSlide(index = 1)
                    .semantics { heading() },
            )

            Spacer(Modifier.height(Theme.spacing.xs))

            // ── Performance message ─────────────────────────────────────
            Text(
                text = stringResource(tier.messageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = Theme.spacing.md)
                    .staggeredFadeSlide(index = 2),
            )

            Spacer(Modifier.height(Theme.spacing.xxl))

            // ── Stats cards ─────────────────────────────────────────────
            StatsSection(
                knownCount = knownCount,
                unknownCount = unknownCount,
                totalCount = totalCount,
                modifier = Modifier.staggeredFadeSlide(index = 3),
            )

            Spacer(Modifier.height(Theme.spacing.xxl))

            // ── Streak badge ────────────────────────────────────────────
            if (newStreak != null && newStreak > 0) {
                StreakBadge(
                    streakDays = newStreak,
                    modifier = Modifier.staggeredFadeSlide(index = 4),
                )
                Spacer(Modifier.height(Theme.spacing.md))
            }

            // ── Missed words ────────────────────────────────────────────
            if (missedWords.isNotEmpty()) {
                MissedWordsSection(
                    words = missedWords,
                    modifier = Modifier.staggeredFadeSlide(index = 5),
                )
                Spacer(Modifier.height(Theme.spacing.xxl))
            }

            // ── Done button ─────────────────────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Theme.dimensions.buttonHeight)
                    .staggeredFadeSlide(index = 6),
                shape = RoundedCornerShape(Theme.shapes.medium),
            ) {
                Text(
                    text = stringResource(Res.string.done),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(Theme.spacing.xxxl))
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.pill),
        color = AppColors.tertiary.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Theme.spacing.lg, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.completion_streak_days, streakDays),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.tertiary,
            )
        }
    }
}

// ─── Missed Words Section ────────────────────────────────────────────────────

@Composable
private fun MissedWordsSection(
    words: List<Word>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.md)) {
            Text(
                text = stringResource(Res.string.completion_missed_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Theme.spacing.xs),
            )
            words.forEachIndexed { index, word ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = Theme.spacing.xxs),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Theme.spacing.xxs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Theme.spacing.sm),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
