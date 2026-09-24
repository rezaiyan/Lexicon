package feature.insights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import components.scaffold.ActionIconConfig
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import components.ErrorScreen
import components.LoadingScreen
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import core.common.UiState
import events.OnEvents
import feature.insights.InsightsEffect
import feature.insights.InsightsState
import feature.insights.InsightsViewModel
import kotlinx.datetime.minus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.insights_load_error
import lexicon.resources.generated.resources.insights_loading
import lexicon.resources.generated.resources.insights_title
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Suppress("UnusedParameter")
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
    onNavigateToReview: (List<Long>) -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val viewModel = koinViewModel<InsightsViewModel>()
    val state by viewModel.state()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is InsightsEffect.NavigateToReviewWithWords -> onNavigateToReview(effect.wordIds)
            InsightsEffect.NavigateToNotificationSettings -> onNavigateToNotificationSettings()
        }
    }

    InsightsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onShowLeaderboard = onShowLeaderboard,
        onDismissInsight = { viewModel.dismissDailyInsight() },
        onRetry = { viewModel.refresh() },
        onStudyDifficultWords = viewModel::studyDifficultWords,
        onSetReminder = viewModel::setReminder,
    )
}

@Composable
internal fun InsightsContent(
    state: InsightsState,
    onNavigateBack: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
    onDismissInsight: () -> Unit = {},
    onRetry: () -> Unit = {},
    onStudyDifficultWords: () -> Unit = {},
    onSetReminder: (Boolean) -> Unit = {},
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_title),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        onNavigationClick = onNavigateBack,
        scrollable = false,
        topBarColor = TopBarColor.Background,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Rounded.EmojiEvents,
            contentDescription = stringResource(Res.string.leaderboard),
            onClick = onShowLeaderboard,
            size = Theme.dimensions.iconSize,
        ),
    ) {
        if (!state.isLoaded) {
            LoadingScreen(message = stringResource(Res.string.insights_loading))
        } else if (state.isError) {
            ErrorScreen(
                message = stringResource(Res.string.insights_load_error),
                retryLabel = stringResource(Res.string.retry),
                onRetry = onRetry,
            )
        } else if (!state.availability.hasAnyContent) {
            EmptyInsightsContent()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Theme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
            ) {
                val currentStreak = state.currentStreak
                if (currentStreak != null) {
                    StreakCard(
                        currentStreak = currentStreak,
                        longestStreak = state.longestStreak,
                    )
                }
                val weeklyReport = (state.weeklyReport as? UiState.Loaded)?.value
                if (weeklyReport is feature.insights.WeeklyReportUiModel.Content) {
                    WeeklyReportCard(report = weeklyReport)
                }
                OverviewTab(state, onDismissInsight, onSetReminder)
                if (state.availability.hasWordRush) {
                    WordRushInsightsSection(state)
                }
                TrendsTab(state)
                WordsTab(state, onStudyDifficultWords)
                Spacer(modifier = Modifier.height(Theme.spacing.xl))
            }
        }
    }
}
