package presentation.ui.components.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.total_reviews_format
import lexicon.resources.generated.resources.weekly_activity
import org.jetbrains.compose.resources.stringResource
import presentation.model.DayActivityUiModel
import presentation.ui.screens.study.rememberAnimatedCounter
import theme.Theme

@Composable
fun WeeklyActivitySection(
    weeklyActivity: List<DayActivityUiModel>,
    modifier: Modifier = Modifier
) {
    val maxReviewCount = weeklyActivity.maxOfOrNull { it.reviewCount } ?: 1
    val barMaxHeight = 72.dp
    val totalReviews = weeklyActivity.sumOf { it.reviewCount }
    val animatedTotal = rememberAnimatedCounter(target = totalReviews)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    primaryColor.copy(alpha = 0.15f),
                    tertiaryColor.copy(alpha = 0.10f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.weekly_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(Res.string.total_reviews_format, animatedTotal),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyActivity.forEachIndexed { index, day ->
                    DayBar(
                        day = day,
                        maxReviewCount = maxReviewCount,
                        barMaxHeight = barMaxHeight,
                        index = index
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBar(
    day: DayActivityUiModel,
    maxReviewCount: Int,
    barMaxHeight: androidx.compose.ui.unit.Dp,
    index: Int,
    modifier: Modifier = Modifier
) {
    val barFraction = if (maxReviewCount > 0) {
        day.reviewCount.toFloat() / maxReviewCount
    } else 0f

    val targetFraction = barFraction.coerceAtLeast(0.06f)
    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 60).toLong())
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    }

    val barHeight = barMaxHeight * animatedFraction.value
    val primaryColor = MaterialTheme.colorScheme.primary
    val barBrush = if (day.isToday) {
        Brush.verticalGradient(
            listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
        )
    } else {
        Brush.verticalGradient(
            listOf(
                primaryColor.copy(alpha = 0.45f),
                primaryColor.copy(alpha = 0.2f)
            )
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        }

        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall4))

        Box(
            modifier = Modifier
                .width(28.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(barBrush)
        )

        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))

        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Today dot indicator
        if (day.isToday) {
            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall4))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}
