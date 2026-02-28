package presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import theme.Theme

@Composable
fun AnimatedNavIcon(
    icon: ImageVector,
    contentDescription: String?,
    selected: Boolean
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Scale up when selected
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconScale"
    )

    // Lift up a bit when selected
    val lift by animateFloatAsState(
        targetValue = if (selected) -2f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconLift"
    )

    // Tint color animation
    val tint by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconTint"
    )

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(Theme.dimensions.iconSize)
            .scale(scale)
            .offset { IntOffset(x = 0, y = lift.roundToInt()) },
        tint = tint
    )
}
