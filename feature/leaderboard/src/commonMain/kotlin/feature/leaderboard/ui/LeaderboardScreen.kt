package feature.leaderboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
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

    // Lazy list layout: [refresh banner] standing [podium] rankings-header rows...
    val standingIndex = if (refreshFailed) 1 else 0
    val podiumIndex = if (data.podium.isNotEmpty()) standingIndex + 1 else null
    val firstRowIndex = (podiumIndex ?: standingIndex) + 2
    val userItemIndex = when {
        data.userRowIndex != null -> firstRowIndex + data.userRowIndex
        data.podium.any { it.isCurrentUser } -> podiumIndex
        else -> null
    }
    // The pill brings the user back to their row, or to their standing card when they have no row.
    val pillTargetIndex = userItemIndex ?: standingIndex
    val showFloatingPill by remember(listState, userItemIndex, standingIndex) {
        derivedStateOf {
            !listState.isItemVisible(standingIndex) && !listState.isItemVisible(userItemIndex)
        }
    }
    val pillPointsDown by remember(listState, pillTargetIndex) {
        derivedStateOf { pillTargetIndex > listState.firstVisibleItemIndex }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Theme.spacing.xs, bottom = Theme.spacing.xxxl),
        ) {
            if (refreshFailed) {
                item(key = "refresh_failed") {
                    Box(modifier = Modifier.padding(bottom = Theme.spacing.md)) {
                        RefreshFailedBanner(onRetry = onRetry)
                    }
                }
            }
            item(key = "standing") {
                Box(modifier = Modifier.padding(bottom = Theme.spacing.lg)) {
                    val userEntry = data.userEntry
                    if (userEntry != null) {
                        YourStandingCard(
                            entry = userEntry,
                            nextUp = data.nextUp,
                            onShowMe = if (data.userRowIndex != null && userItemIndex != null) {
                                { scope.launch { listState.animateScrollToItem(userItemIndex) } }
                            } else {
                                null
                            },
                        )
                    } else {
                        UnrankedStandingCard()
                    }
                }
            }
            if (data.podium.isNotEmpty()) {
                item(key = "podium") {
                    Box(modifier = Modifier.padding(bottom = Theme.spacing.lg)) {
                        LeaderboardPodium(entries = data.podium)
                    }
                }
            }
            if (data.rows.isNotEmpty()) {
                item(key = "rankings_header") {
                    RankingsHeader(count = data.podium.size + data.rows.size)
                }
            }
            // Ranks can tie, so the index keeps keys unique.
            itemsIndexed(data.rows, key = { index, entry -> "row-${entry.rank}-$index" }) { index, entry ->
                RankingRow(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == data.rows.lastIndex,
                )
            }
        }

        val userEntry = data.userEntry
        AnimatedVisibility(
            visible = userEntry != null && showFloatingPill,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Theme.spacing.md),
        ) {
            if (userEntry != null) {
                FloatingYouPill(
                    entry = userEntry,
                    pointsDown = pillPointsDown,
                    onClick = { scope.launch { listState.animateScrollToItem(pillTargetIndex) } },
                )
            }
        }
    }
}

private fun LazyListState.isItemVisible(index: Int?): Boolean =
    index != null && layoutInfo.visibleItemsInfo.any { it.index == index }
