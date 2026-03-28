package components.scaffold

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.navigate_back

@Composable
fun LexiconColumn(
    title: String? = null,
    showNavigationIcon: Boolean = false,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    actionIcon1: ActionIconConfig? = null,
    actionIcon2: ActionIconConfig? = null,
    actionIcon3: ActionIconConfig? = null,
    scrollable: Boolean = true,
    scrollState: ScrollState? = null,
    topBarColor: TopBarColor = TopBarColor.Background,
    collapsedContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val hasTopBar =
        title != null || showNavigationIcon || actionIcon1 != null || actionIcon2 != null || actionIcon3 != null

    if (scrollable) {
        val resolvedScrollState = scrollState ?: rememberScrollState()
        Column(
            Modifier.background(Theme.colors.background)
        ) {
            if (hasTopBar) {
                FlexibleTopBar(
                    title = title,
                    showNavigationIcon = showNavigationIcon,
                    navigationIcon = navigationIcon,
                    navigationIconContentDescription = navigationIconContentDescription,
                    onNavigationClick = onNavigationClick,
                    actionIcon1 = actionIcon1,
                    actionIcon2 = actionIcon2,
                    actionIcon3 = actionIcon3,
                    topBarColor = topBarColor,
                    collapsedContent = collapsedContent,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = Theme.spacing.medium)
                    .verticalScroll(resolvedScrollState)
            ) {
                content()
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            if (hasTopBar) {
                FlexibleTopBar(
                    title = title,
                    showNavigationIcon = showNavigationIcon,
                    navigationIcon = navigationIcon,
                    navigationIconContentDescription = navigationIconContentDescription,
                    onNavigationClick = onNavigationClick,
                    actionIcon1 = actionIcon1,
                    actionIcon2 = actionIcon2,
                    actionIcon3 = actionIcon3,
                    topBarColor = topBarColor,
                    collapsedContent = collapsedContent,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = Theme.spacing.medium)
            ) {
                content()
            }
        }
    }

}

@Composable
private fun FlexibleTopBar(
    title: String?,
    showNavigationIcon: Boolean,
    navigationIcon: ImageVector,
    navigationIconContentDescription: String? = null,
    onNavigationClick: () -> Unit,
    actionIcon1: ActionIconConfig?,
    actionIcon2: ActionIconConfig?,
    actionIcon3: ActionIconConfig? = null,
    topBarColor: TopBarColor,
    collapsedContent: (@Composable () -> Unit)? = null,
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
            collapsedContent?.invoke()
        },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = navigationIconContentDescription
                            ?: stringResource(Res.string.navigate_back)
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

            actionIcon3?.let { config ->
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
    val size: Dp = 24.dp
)

sealed class TopBarColor {
    data object Default : TopBarColor()
    data object Surface : TopBarColor()
    data object Background : TopBarColor()
}
