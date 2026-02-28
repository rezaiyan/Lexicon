package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

import theme.Theme

/**
 * Section header with a bold title and an optional count badge pill.
 *
 * Used for titled sections like "Learning Stages (24)", "Statistics", etc.
 *
 * @param title The section title text.
 * @param modifier Optional modifier.
 * @param count Optional numeric badge shown next to the title. Hidden when null or 0.
 * @param badgeColor The badge text & tinted background color. Defaults to primary.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor,
                modifier = Modifier
                    .background(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(Theme.shapes.small)
                    )
                    .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs)
            )
        }
    }
}
