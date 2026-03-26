package feature.study.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReviewButton(
    text: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val isVerySmall = maxWidth < 70.dp
        val isSmall = maxWidth < 90.dp
        val buttonHeight = when {
            isVerySmall -> 60.dp
            icon != null -> 80.dp
            else -> 72.dp
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = color.copy(alpha = 0.15f),
                contentColor = color,
                disabledContainerColor = color.copy(alpha = 0.06f),
                disabledContentColor = color.copy(alpha = 0.3f),
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(
                horizontal = if (isVerySmall) 2.dp else if (isSmall) 4.dp else 8.dp,
                vertical = 8.dp
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null && !isVerySmall) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = color,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    style = when {
                        isVerySmall -> MaterialTheme.typography.labelSmall
                        isSmall -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.labelLarge
                    },
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.65f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
