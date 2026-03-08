package components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import theme.Theme

/**
 * A pill-style segmented tab selector with animated selection state.
 *
 * Each tab occupies equal width. The selected tab gets a raised surface with
 * primary content color; unselected tabs are transparent with muted color.
 *
 * @param T The tab data type.
 * @param tabs List of tab items to display.
 * @param selectedIndex Index of the currently selected tab.
 * @param onTabSelected Called with the index and tab when a tab is tapped.
 * @param modifier Optional modifier for the outer container.
 * @param tabContent Slot for rendering each tab's content (icon, text, etc.).
 *        Receives the tab item — content color is provided via [LocalContentColor]
 *        by the wrapping [Surface].
 */
@Composable
fun <T> TabHost(
    tabs: List<T>,
    selectedIndex: Int,
    onTabSelected: (index: Int, tab: T) -> Unit,
    modifier: Modifier = Modifier,
    tabContent: @Composable (tab: T, isSelected: Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Theme.shapes.medium),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeightSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface
                    else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tab_bg_$index"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tab_content_$index"
                )

                Surface(
                    onClick = { onTabSelected(index, tab) },
                    color = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.height(Theme.dimensions.buttonHeightSmall),
                        horizontalArrangement = Arrangement.spacedBy(
                            Theme.spacing.xxs,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabContent(tab, isSelected)
                    }
                }
            }
        }
    }
}
