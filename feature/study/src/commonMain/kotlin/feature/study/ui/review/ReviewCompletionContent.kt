package feature.study.ui.review

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.animation.ConfettiOverlay
import components.animation.rememberAnimatedCounter
import components.animation.staggeredFadeSlide
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.completion_cards_reviewed
import lexicon.resources.generated.resources.completion_forgot
import lexicon.resources.generated.resources.completion_good_message
import lexicon.resources.generated.resources.completion_good_title
import lexicon.resources.generated.resources.completion_great_message
import lexicon.resources.generated.resources.completion_great_title
import lexicon.resources.generated.resources.completion_okay_message
import lexicon.resources.generated.resources.completion_okay_title
import lexicon.resources.generated.resources.completion_perfect_message
import lexicon.resources.generated.resources.completion_perfect_title
import lexicon.resources.generated.resources.completion_remembered
import lexicon.resources.generated.resources.completion_score_percent
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
                modifier = Modifier.staggeredFadeSlide(index = 1),
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

            // ── Done button ─────────────────────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Theme.dimensions.buttonHeight)
                    .staggeredFadeSlide(index = 4),
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

// ─── Score Ring ──────────────────────────────────────────────────────────────

@Composable
private fun ScoreRing(
    scorePercent: Int,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val sweepTarget = scorePercent / 100f
    val animatedSweep = remember { Animatable(0f) }
    val animatedCount = rememberAnimatedCounter(scorePercent, durationMillis = 1200)

    LaunchedEffect(sweepTarget) {
        animatedSweep.animateTo(
            targetValue = sweepTarget,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gradientBrush = Brush.sweepGradient(gradientColors)

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val padding = strokeWidth / 2

            val arcTopLeft = androidx.compose.ui.geometry.Offset(padding, padding)
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - strokeWidth,
                size.height - strokeWidth,
            )

            // Track (soft, semi-transparent)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = arcTopLeft,
                size = arcSize,
            )

            // Gradient progress arc
            drawArc(
                brush = gradientBrush,
                startAngle = -90f,
                sweepAngle = 360f * animatedSweep.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = arcTopLeft,
                size = arcSize,
            )
        }

        // Score percentage text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.completion_score_percent, animatedCount),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ─── Stats Section ──────────────────────────────────────────────────────────

@Composable
private fun StatsSection(
    knownCount: Int,
    unknownCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val animatedKnown = rememberAnimatedCounter(knownCount)
    val animatedUnknown = rememberAnimatedCounter(unknownCount)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        // Total cards label
        Text(
            text = stringResource(Res.string.completion_cards_reviewed, totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Proportional bar
        ProportionalBar(
            knownCount = knownCount,
            unknownCount = unknownCount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.md),
        )

        Spacer(Modifier.height(Theme.spacing.xxs))

        // Stat cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            StatCard(
                count = animatedKnown,
                label = stringResource(Res.string.completion_remembered),
                accentColor = Theme.colors.success,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                count = animatedUnknown,
                label = stringResource(Res.string.completion_forgot),
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─── Proportional Bar ───────────────────────────────────────────────────────

@Composable
private fun ProportionalBar(
    knownCount: Int,
    unknownCount: Int,
    modifier: Modifier = Modifier,
) {
    val total = knownCount + unknownCount
    if (total == 0) return

    val knownFraction = knownCount.toFloat() / total
    val successColor = Theme.colors.success
    val errorColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        if (knownCount > 0) {
            Box(
                modifier = Modifier
                    .weight(knownFraction)
                    .height(8.dp)
                    .background(successColor),
            )
        }
        if (unknownCount > 0) {
            Box(
                modifier = Modifier
                    .weight(1f - knownFraction)
                    .height(8.dp)
                    .background(errorColor),
            )
        }
    }
}

// ─── Stat Card ──────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    count: Int,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.medium),
        color = Theme.colors.surfaceContainerLow,
        tonalElevation = Theme.elevation.low,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Theme.spacing.md,
                vertical = Theme.spacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor),
            )

            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Performance Tiers ──────────────────────────────────────────────────────

private enum class PerformanceTier(
    val titleRes: org.jetbrains.compose.resources.StringResource,
    val messageRes: org.jetbrains.compose.resources.StringResource,
    val gradientColors: List<Color>,
) {
    PERFECT(
        titleRes = Res.string.completion_perfect_title,
        messageRes = Res.string.completion_perfect_message,
        gradientColors = listOf(AppColors.secondary, Color(0xFF34D399)),
    ),
    GREAT(
        titleRes = Res.string.completion_great_title,
        messageRes = Res.string.completion_great_message,
        gradientColors = listOf(AppColors.primary, Color(0xFFA78BFA)),
    ),
    GOOD(
        titleRes = Res.string.completion_good_title,
        messageRes = Res.string.completion_good_message,
        gradientColors = listOf(AppColors.primary, Color(0xFF60A5FA)),
    ),
    OKAY(
        titleRes = Res.string.completion_okay_title,
        messageRes = Res.string.completion_okay_message,
        gradientColors = listOf(AppColors.tertiary, Color(0xFFFBBF24)),
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
