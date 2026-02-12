package presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.navigate_back

/**
 * Reusable scaffold layout for Lexicon app screens
 * Provides consistent structure with optional components
 *
 * Features:
 * - Optional top app bar with title
 * - Optional navigation icon (back button)
 * - Optional divider below app bar
 * - Up to 2 action icons
 * - Scroll behavior (hide/show on scroll)
 * - Consistent padding system
 * - Safe area handling
 */
@Composable
fun LexiconColumn(
    title: String? = null,
    showNavigationIcon: Boolean = false,
    onNavigationClick: () -> Unit = {},
    actionIcon1: ActionIconConfig? = null,
    actionIcon2: ActionIconConfig? = null,
    scrollable: Boolean = true,
    topBarColor: TopBarColor = TopBarColor.Background,
    content: @Composable () -> Unit
) {

    if (scrollable) {
        Column {
            val hasTopBar =
                title != null || showNavigationIcon || actionIcon1 != null || actionIcon2 != null

            if (hasTopBar) {
                FlexibleTopBar(
                    title = title,
                    showNavigationIcon = showNavigationIcon,
                    onNavigationClick = onNavigationClick,
                    actionIcon1 = actionIcon1,
                    actionIcon2 = actionIcon2,
                    topBarColor = topBarColor
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = Theme.spacing.medium)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    } else {
        Column {
            val hasTopBar =
                title != null || showNavigationIcon || actionIcon1 != null || actionIcon2 != null

            if (hasTopBar) {
                FlexibleTopBar(
                    title = title,
                    showNavigationIcon = showNavigationIcon,
                    onNavigationClick = onNavigationClick,
                    actionIcon1 = actionIcon1,
                    actionIcon2 = actionIcon2,
                    topBarColor = topBarColor
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = Theme.spacing.medium)
            ) {
                content()
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlexibleTopBar(
    title: String?,
    showNavigationIcon: Boolean,
    onNavigationClick: () -> Unit,
    actionIcon1: ActionIconConfig?,
    actionIcon2: ActionIconConfig?,
    topBarColor: TopBarColor
) {
    TopAppBar(
        modifier = Modifier.padding(horizontal = Theme.spacing.extraSmall),
        title = {
            if (title != null) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.navigate_back)
                    )
                }
            }
        },
        actions = {
            actionIcon1?.let { config ->
                IconButton(onClick = config.onClick) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.contentDescription,
                        tint = config.tint ?: LocalContentColor.current,
                        modifier = Modifier.size(config.size)
                    )
                }
            }

            actionIcon2?.let { config ->
                IconButton(onClick = config.onClick) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.contentDescription,
                        tint = config.tint ?: LocalContentColor.current,
                        modifier = Modifier.size(config.size)
                    )
                }
            }
        },
        colors = when (topBarColor) {
            TopBarColor.Default -> TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
            )

            TopBarColor.Surface -> TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            )

            TopBarColor.Background -> TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            )
        }
    )
}

data class ActionIconConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val tint: Color? = null,
    val size: Dp = 24.dp  // Theme.spacing.xxl
)

sealed class TopBarColor {
    data object Default : TopBarColor()
    data object Surface : TopBarColor()
    data object Background : TopBarColor()
}