package presentation.ui.components.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import theme.Theme

@Composable
fun ImportMethodSelectorContent(
    onManual: () -> Unit,
    onAiAssistant: () -> Unit,
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Text(
                text = "Add Words",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose how you'd like to build your vocabulary",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AiAssistantCard(onAiAssistant, spacing, dimensions)

        OrDivider(spacing)

        ManualImportCard(onManual, spacing, dimensions)
    }
}

@Composable
private fun AiAssistantCard(
    onClick: () -> Unit,
    spacing: theme.AppSpacing,
    dimensions: theme.AppDimensions,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cardCornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(spacing.small)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensions.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.none)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(Theme.dimensions.iconSizeLarge)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Get personalized vocabulary tailored to your level and interests",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                )
            }
        }
    }
}

@Composable
private fun OrDivider(spacing: theme.AppSpacing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "or",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ManualImportCard(
    onClick: () -> Unit,
    spacing: theme.AppSpacing,
    dimensions: theme.AppDimensions,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(Theme.dimensions.borderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSize)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Manually",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Type, paste, or import from file or image",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
            )
        }
    }
}
