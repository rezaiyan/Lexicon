package feature.profile.ui.components

import components.LottieMotionIcon
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.streak.model.StreakData
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.best_streak
import lexicon.resources.generated.resources.day_streak
import org.jetbrains.compose.resources.stringResource
import components.animation.rememberAnimatedCounter
import theme.Theme

private const val FIRE_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/9d140e5e-1121-11ef-a147-0f8f2c5fd446/12M9FMZfjS.json"

@Composable
fun StreakSection(
    streak: StreakData,
    longestStreak: Int? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
    ) {
        StreakCard(
            first = streak.currentStreak,
            firstLabel = stringResource(Res.string.day_streak),
            second = longestStreak,
            secondLabel = stringResource(Res.string.best_streak),
            accentColor = primaryColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StreakCard(
    first: Int,
    firstLabel: String,
    second: Int? = null,
    secondLabel: String? = null,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedFirst = rememberAnimatedCounter(target = first)
    val animatedSecond = rememberAnimatedCounter(target = second ?: 0)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val cardShape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .height(88.dp)
            .clip(cardShape)
            .background(surfaceColor.copy(alpha = 0.9f))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.06f)
                        )
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(start = Theme.spacing.xxs, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Lottie fire animation on the left
        Box(
            modifier = Modifier
                .padding(bottom = Theme.spacing.extraSmall)
                .size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            LottieMotionIcon(
                url = FIRE_LOTTIE_URL,
                modifier = Modifier.size(64.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedFirst",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
            Text(
                text = firstLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp
            )
        }

        if (second != null && secondLabel != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedSecond",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = secondaryColor,
                    lineHeight = 30.sp
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
                Text(
                    text = secondLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
