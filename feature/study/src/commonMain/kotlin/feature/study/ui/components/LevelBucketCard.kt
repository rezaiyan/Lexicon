package feature.study.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import theme.Theme

@Composable
fun LevelBucketCard(
    level: String,
    description: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isEmpty = count == 0

    val wordLabel = if (count == 1) "word" else "words"
    Card(
        modifier = modifier
            .semantics {
                contentDescription = "$level: $count $wordLabel. $description"
            }
            .drawBehind {
                // Left accent bracket drawn directly on the card
                val strokeWidth = 3.5.dp.toPx()
                val bracketWidth = 14.dp.toPx()
                val verticalPadding = size.height * 0.15f
                val curveDepth = bracketWidth * 0.3f

                val path = Path().apply {
                    moveTo(bracketWidth, verticalPadding)
                    cubicTo(
                        curveDepth, size.height * 0.3f,
                        curveDepth, size.height * 0.7f,
                        bracketWidth, size.height - verticalPadding
                    )
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
            .fillMaxWidth()
            .combinedClickable(enabled = !isEmpty, onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEmpty) 0.dp else Theme.elevation.low
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEmpty) 0.4f else 1f)
                .padding(start = 18.dp, end = Theme.spacing.md)
                .padding(vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in colored circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = color.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Theme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
            ) {
                Text(
                    text = level,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Count + label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (count == 1) "WORD" else "WORDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
