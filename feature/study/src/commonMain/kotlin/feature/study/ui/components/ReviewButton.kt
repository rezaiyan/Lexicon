package feature.study.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import theme.Theme

/**
 * Rating button: icon, then a bold label over a smaller sub-label, on a 64dp, 20dp-radius surface.
 * [containerColor] / [contentColor] choose between tonal (forgot) and filled (remembered) styles.
 */
@Composable
fun ReviewButton(
    text: String,
    subText: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BoxWithConstraints(modifier = modifier) {
        // Narrow buttons (small screens / large fonts) drop the icon so the labels keep their room.
        val showIcon = maxWidth / LocalDensity.current.fontScale >= IconMinButtonWidth
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
            contentPadding = PaddingValues(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
        ) {
            if (showIcon) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(Theme.spacing.xs))
            }
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val IconMinButtonWidth = 130.dp
