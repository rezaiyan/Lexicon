package presentation.ui.components.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.model.DayActivityUiModel
import theme.Theme

private val BarMaxHeight = 120.dp
private val BarWidth = 32.dp
private val ShadowOffset = 5.dp

@Composable
fun WeeklyActivitySection(
    weeklyActivity: List<DayActivityUiModel>,
    modifier: Modifier = Modifier
) {
    val maxReviewCount = weeklyActivity.maxOfOrNull { it.reviewCount } ?: 1

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = Theme.elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyActivity.forEachIndexed { index, day ->
                DayBar(
                    day = day,
                    maxReviewCount = maxReviewCount,
                    barMaxHeight = BarMaxHeight,
                    barWidth = BarWidth,
                    index = index
                )
            }
        }
    }
}

@Composable
private fun DayBar(
    day: DayActivityUiModel,
    maxReviewCount: Int,
    barMaxHeight: Dp,
    barWidth: Dp,
    index: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (maxReviewCount > 0) {
        day.reviewCount.toFloat() / maxReviewCount
    } else 0f

    // Minimum fraction so even zero-activity days show a small circle
    val targetFraction = fraction.coerceAtLeast(0.15f)
    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 60).toLong())
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
    }

    val barHeight = barMaxHeight * animatedFraction.value
    // Shadow bar is ~20% taller than the main bar
    val shadowHeight = barMaxHeight * (animatedFraction.value * 1.5f).coerceAtMost(1f)

    val primaryColor = MaterialTheme.colorScheme.primary
    val shadowColor = MaterialTheme.colorScheme.surfaceContainerHigh

    // Intensity varies: today or highest gets full color, others fade
    val barAlpha = if (day.isToday) 1f else (0.4f + fraction * 0.6f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Review count label above the bar
        if (day.reviewCount > 0) {
            Text(
                text = day.reviewCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (day.isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
        }

        // Overlapping bars container — shadow sits behind, main bar overlaps on top
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            // Shadow/background capsule — peeks out left and above
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(shadowHeight)
                    .clip(RoundedCornerShape(50))
                    .background(shadowColor)
            )

            // Main capsule — shifted slightly right, layered on top
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(primaryColor.copy(alpha = barAlpha))
            )
        }

        Spacer(modifier = Modifier.height(Theme.spacing.xs))

        Text(
            text = day.dayOfWeekLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (day.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
