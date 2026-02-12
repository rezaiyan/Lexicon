package presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.open

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    iconTint: Color? = null,
    subtitleColor: Color? = null,
    showTrailingArrow: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSize),
                    tint = iconTint ?: LocalContentColor.current
                )
                
                Spacer(modifier = Modifier.width(Theme.spacing.extraSmall))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (subtitle != null) {
                        Text(
                            modifier = Modifier.padding(end = Theme.spacing.extraSmall2),
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (trailingContent != null) {
                trailingContent()
            } else if (showTrailingArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(Res.string.open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}