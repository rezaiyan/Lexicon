package feature.study.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.words_unit
import org.jetbrains.compose.resources.pluralStringResource
import theme.Theme

/**
 * Card for a learning stage or tag bucket: tinted icon circle, optional [overline]
 * (e.g. "LEVEL 1"), title, optional description, word count and a chevron.
 *
 * Non-empty buckets get a thin ring in [color]; empty ones are dimmed and not clickable.
 */
@Composable
fun LevelBucketCard(
    level: String,
    description: String?,
    count: Int,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    overline: String? = null,
    modifier: Modifier = Modifier,
) {
    val isEmpty = count == 0

    val wordUnit = pluralStringResource(Res.plurals.words_unit, count)
    Card(
        modifier = modifier
            .semantics {
                contentDescription = listOfNotNull("$level: $count $wordUnit", description).joinToString(". ")
            }
            .fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isEmpty) {
            null
        } else {
            BorderStroke(Theme.dimensions.borderWidth, color.copy(alpha = Theme.opacity.dimming))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEmpty) 0.dp else Theme.elevation.low
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(enabled = !isEmpty, onClick = onClick, onLongClick = onLongClick)
                .alpha(if (isEmpty) Theme.opacity.hint else 1f)
                .padding(Theme.spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.inlineGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in colored circle
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSizeHuge)
                    .background(
                        color = color.copy(alpha = Theme.opacity.focus),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(Theme.dimensions.iconSize)
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
            ) {
                if (overline != null) {
                    Text(
                        text = overline,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                    )
                }
                Text(
                    text = level,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Count + label
            Column(
                modifier = Modifier.widthIn(min = Theme.dimensions.touchTarget),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
            ) {
                val countColor = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else color
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = countColor,
                )
                Text(
                    text = wordUnit.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    color = countColor,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
            )
        }
    }
}
