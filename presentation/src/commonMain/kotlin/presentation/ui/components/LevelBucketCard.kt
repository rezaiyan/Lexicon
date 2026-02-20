package presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kodein.emoji.compose.m3.TextWithNotoImageEmoji
import theme.Theme

@Composable
fun LevelBucketCard(
    level: String,
    description: String,
    count: Int,
    color: Color,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    totalWords: Int = 0
) {
    val isEmpty = count == 0
    val cardAlpha = if (isEmpty) 0.4f else 1f
    val fraction = if (totalWords > 0) (count.toFloat() / totalWords).coerceIn(0f, 1f) else 0f

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "levelProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = !isEmpty,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = if (isEmpty) 0.07f else 0.15f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.spacing.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextWithNotoImageEmoji(
                        text = icon,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = Theme.spacing.cardPadding),
                        color = Color.Unspecified.copy(alpha = cardAlpha),
                        maxLines = 1
                    )
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = count.toString(),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isEmpty) color.copy(alpha = 0.25f) else color,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Animated relative progress bar
            if (totalWords > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.cardPadding)
                        .padding(bottom = Theme.spacing.extraSmall3)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .height(3.dp)
                            .background(color.copy(alpha = if (isEmpty) 0.2f else 0.65f))
                    )
                }
            }
        }
    }
}
