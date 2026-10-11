package feature.leaderboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import core.common.UiState
import feature.leaderboard.LeaderboardViewModel
import feature.leaderboard.model.LeaderboardState
import feature.leaderboard.model.LeaderboardUiData
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.content_description_back
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.leaderboard_empty_subtitle
import lexicon.resources.generated.resources.leaderboard_empty_title
import lexicon.resources.generated.resources.leaderboard_error_title
import lexicon.resources.generated.resources.refresh
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun LeaderboardScreen(
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<LeaderboardViewModel>()
    val state by viewModel.state()

    LexiconColumn(
        title = stringResource(Res.string.leaderboard),
        showNavigationIcon = true,
        navigationIconContentDescription = stringResource(Res.string.content_description_back),
        onNavigationClick = onDismiss,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Refresh,
            contentDescription = stringResource(Res.string.refresh),
            onClick = viewModel::refresh,
            size = Theme.dimensions.iconSize
        ),
        scrollable = false
    ) {
        LeaderboardScreenContent(
            state = state,
            onRefresh = viewModel::refresh,
        )
    }
}

@Composable
private fun LeaderboardScreenContent(
    state: LeaderboardState,
    onRefresh: () -> Unit,
) {
    when (val content = state.content) {
        is UiState.Loading -> LoadingScreen()

        is UiState.Error -> ErrorScreen(
            title = stringResource(Res.string.leaderboard_error_title),
            message = content.message,
            icon = Icons.Rounded.CloudOff,
            retryLabel = stringResource(Res.string.retry),
            onRetry = onRefresh,
        )

        is UiState.Loaded -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val data = content.value
            if (data.isEmpty) {
                // EmptyScreen scrolls vertically, so pull-to-refresh still works here.
                EmptyScreen(
                    title = stringResource(Res.string.leaderboard_empty_title),
                    subtitle = stringResource(Res.string.leaderboard_empty_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            } else {
                LeaderboardContent(
                    data = data,
                    refreshFailed = state.refreshFailed,
                    onRetry = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun LeaderboardContent(
    data: LeaderboardUiData,
    refreshFailed: Boolean,
    onRetry: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Lazy list layout: [refresh banner] [podium] legend rows...
    val bannerCount = if (refreshFailed) 1 else 0
    val podiumIndex = if (data.podium.isNotEmpty()) bannerCount else null
    val firstRowIndex = bannerCount + (if (podiumIndex != null) 1 else 0) + 1
    val userItemIndex = when {
        data.userRowIndex != null -> firstRowIndex + data.userRowIndex
        data.podium.any { it.isCurrentUser } -> podiumIndex
        else -> null
    }
    val isUserItemVisible by remember(listState, userItemIndex) {
        derivedStateOf { listState.isItemVisible(userItemIndex) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            if (refreshFailed) {
                item(key = "refresh_failed") {
                    RefreshFailedBanner(onRetry = onRetry)
                }
            }
            if (data.podium.isNotEmpty()) {
                item(key = "podium") {
                    LeaderboardPodium(entries = data.podium)
                }
            }
            item(key = "legend") {
                StatsLegend()
            }
            // Ranks can tie, so the index keeps keys unique.
            itemsIndexed(data.rows, key = { index, entry -> "row-${entry.rank}-$index" }) { _, entry ->
                LeaderboardEntryCard(entry = entry)
            }
        }

        AnimatedVisibility(
            visible = !isUserItemVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            val userEntry = data.userEntry
            if (userEntry != null) {
                UserRankingFooter(
                    entry = userEntry,
                    onJumpToEntry = if (userItemIndex != null) {
                        { scope.launch { listState.animateScrollToItem(userItemIndex) } }
                    } else {
                        null
                    },
                )
            } else {
                UnrankedFooter()
            }
        }
    }
}

private fun LazyListState.isItemVisible(index: Int?): Boolean =
    index != null && layoutInfo.visibleItemsInfo.any { it.index == index }
