package components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import theme.Theme

private val LightGlassTint = Color(0xFFFAFAFA)
private val DarkGlassTint = Color(0xFF121212)
private val BarShape = RoundedCornerShape(percent = 50)

/** A single destination rendered inside [GlassBottomNavBar]. */
data class GlassNavItem(
    val selected: Boolean,
    val label: String,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

/**
 * Bottom navigation bar with a frosted-glass surface — blurs whatever scrolls behind it,
 * captured via [backdropLayer]. Built entirely on Compose's own `GraphicsLayer`/`RenderEffect`
 * APIs, no third-party dependency.
 *
 * Only draw this when [isLiquidGlassSupported] returns true; callers own the fallback
 * (typically a plain Material3 `NavigationBar`) for unsupported platforms/devices.
 *
 * @param backdropLayer the layer capturing the content behind this bar, recorded via
 *   `Modifier.glassBackdropSource` on the screen content.
 */
@Composable
fun GlassBottomNavBar(
    backdropLayer: GraphicsLayer,
    items: List<GlassNavItem>,
    modifier: Modifier = Modifier,
) {
    var barPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    val containerColor = if (isSystemInDarkTheme()) {
        DarkGlassTint.copy(alpha = 0.55f)
    } else {
        LightGlassTint.copy(alpha = 0.55f)
    }

    Box(
        modifier
            .padding(horizontal = Theme.spacing.lg, vertical = Theme.spacing.sm)
            .height(64.dp)
            .onGloballyPositioned { coordinates -> barPositionInRoot = coordinates.positionInRoot() }
            .shadow(elevation = 8.dp, shape = BarShape)
            .clip(BarShape)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = BlurEffect(24.dp.toPx(), 24.dp.toPx(), TileMode.Decal)
                }
                .drawBehind {
                    translate(left = -barPositionInRoot.x, top = -barPositionInRoot.y) {
                        drawLayer(backdropLayer)
                    }
                }
        )
        Box(Modifier.fillMaxSize().background(containerColor))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item -> GlassNavItemButton(item) }
        }
    }
}

@Composable
private fun RowScope.GlassNavItemButton(item: GlassNavItem) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .selectable(
                selected = item.selected,
                onClick = item.onClick,
                role = Role.Tab
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item.icon()
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (item.selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
