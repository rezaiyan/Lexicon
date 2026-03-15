package presentation.ui.components.imports

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import theme.Theme

@Composable
internal fun WordPreviewCard(
    index: Int,
    originalWord: String,
    translation: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "word_card_bg_$index"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "word_card_border_$index"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                selected = isSelected
            },
        shape = RoundedCornerShape(Theme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.none)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
                ) {
                    Text(
                        text = originalWord,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
