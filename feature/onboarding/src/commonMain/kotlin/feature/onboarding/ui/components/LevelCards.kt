package feature.onboarding.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_advanced_desc
import lexicon.resources.generated.resources.onboarding_beginner_desc
import lexicon.resources.generated.resources.onboarding_intermediate_desc
import org.jetbrains.compose.resources.stringResource
import theme.Theme

internal val levelIcons = mapOf(
    "beginner" to Icons.Default.School,
    "intermediate" to Icons.AutoMirrored.Filled.TrendingUp,
    "advanced" to Icons.Default.Star
)

private val SelectionBadgeSize = 22.dp
private val CheckIconSize = 14.dp

@Composable
fun LevelCards(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    val spacing = Theme.spacing
    val levels = listOf(
        Triple("beginner", stringResource(Res.string.onboarding_beginner_desc), levelIcons["beginner"]),
        Triple("intermediate", stringResource(Res.string.onboarding_intermediate_desc), levelIcons["intermediate"]),
        Triple("advanced", stringResource(Res.string.onboarding_advanced_desc), levelIcons["advanced"])
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        levels.forEach { (level, description, icon) ->
            LevelTile(
                level = level,
                description = description,
                icon = icon,
                levelColor = when (level) {
                    "beginner" -> MaterialTheme.colorScheme.secondary
                    "intermediate" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                selected = selectedLevel == level,
                enabled = enabled,
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

@Composable
private fun LevelTile(
    level: String,
    description: String,
    icon: ImageVector?,
    levelColor: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level_bg_$level"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) levelColor
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level_border_$level"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(Theme.shapes.large),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Theme.elevation.medium else Theme.elevation.none
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Theme.spacing.xs)
                        .size(SelectionBadgeSize)
                        .clip(CircleShape)
                        .background(levelColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(CheckIconSize),
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.md, horizontal = Theme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.touchTarget)
                        .clip(RoundedCornerShape(14.dp))
                        .background(levelColor.copy(alpha = if (selected) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                            tint = levelColor
                        )
                    }
                }

                Text(
                    text = level.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) levelColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
