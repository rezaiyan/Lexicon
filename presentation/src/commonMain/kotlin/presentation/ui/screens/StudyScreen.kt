package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import components.ErrorScreen
import components.LoadingScreen
import components.SectionHeader
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import core.common.UiState
import domain.tag.model.Tag
import domain.word.model.LearningStage
import domain.word.model.ReviewSource
import events.OnEvents
import feature.insights.navigation.showInsightsSheet
import feature.leaderboard.navigation.showLeaderboard
import feature.profile.navigation.showProfileSheet
import feature.study.ReviewEffect
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.ui.components.LevelBucketCard
import feature.study.ui.review.ReviewScreen
import feature.study.ui.study.CollapsedStatsBar
import feature.study.ui.study.LearningStagesSection
import feature.study.ui.study.StatsSection
import feature.study.ui.study.WordDistributionBar
import feature.study.ui.wordrush.WordRushCard
import feature.study.ui.wordrush.WordRushGameScreen
import feature.study.wordrush.WordRushEffect
import feature.study.wordrush.WordRushViewModel
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.insights_title
import lexicon.resources.generated.resources.profile
import lexicon.resources.generated.resources.skip_tag_selector_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import overlay.LocalOverlayHost
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.imports.AiWordImportBottomSheet
import presentation.ui.components.imports.ImportBottomSheet
import presentation.ui.components.imports.ImportMethodSelectorContent
import theme.AppColors
import theme.Theme

/** Non-dismissable sheet configuration reused for import flows. */
private val LockedSheetProperties = BottomSheetProperties(
    dismissOnTouchOutside = false,
    dismissOnBackPress = false,
    isNavigationBarsPaddingEnabled = true,
    sheetGesturesEnabled = false,
    showDragHandle = false,
)

private sealed interface ImportFlowPage {
    data object Selector : ImportFlowPage
    data object Manual : ImportFlowPage
    data object AiAssistant : ImportFlowPage
}

@Composable
fun StudyScreen() {
    val progressViewModel = koinViewModel<StudyProgressViewModel>()
    val reviewViewModel = koinViewModel<ReviewViewModel>()
    val wordRushViewModel = koinViewModel<WordRushViewModel>()
    val overlayHost = LocalOverlayHost.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val progressState by progressViewModel.state()
    val uiState = progressState.progress
    val hasPremiumAccess = progressState.hasPremiumAccess
    val dueTags = progressState.dueTags
    val skipTagSelector = progressState.skipTagSelector
    val stageTagsMap = progressState.stageTagsMap

    val scrollState = rememberScrollState()
    var statsSectionBottom by remember { mutableIntStateOf(0) }
    val isStatsSectionScrolledAway = remember(scrollState.value, statsSectionBottom) {
        scrollState.value > statsSectionBottom && statsSectionBottom > 0
    }

    val progressStats = (uiState as? UiState.Loaded)?.value?.progressStats

    val onImportSuccess: (String) -> Unit = { message ->
        progressViewModel.refreshStats()
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // Single entry point for all review flows — eliminates 5+ repetitive call sites.
    val openReviewScreen: (ReviewSource) -> Unit = { source ->
        reviewViewModel.startSession(source)
        overlayHost.showFullScreen(
            tag = "review-${source::class.simpleName}",
            properties = FullScreenProperties(
                dismissOnBackPress = false,
                isNavigationBarsPaddingEnabled = true,
            ),
        ) { navigator ->
            OnEvents(reviewViewModel.effects) { effect ->
                when (effect) {
                    ReviewEffect.SessionComplete -> {
                        progressViewModel.refreshStats()
                        navigator.dismiss()
                    }
                }
            }
            ReviewScreen(
                viewModel = reviewViewModel,
                onDismiss = { reviewViewModel.abandonSession(); navigator.dismiss() },
            )
        }
    }

    val openImportSheet: () -> Unit = {
        if (hasPremiumAccess) {
            overlayHost.showSizeToFitBottomSheet(
                tag = "import",
                properties = BottomSheetProperties(dismissOnBackPress = true, dismissOnTouchOutside = true),
            ) { sheetNav ->
                val pages = rememberBottomSheetPageNavigator<ImportFlowPage>(ImportFlowPage.Selector)
                val onClose: () -> Unit = { sheetNav.dismiss() }

                BottomSheetPages(
                    navigator = pages,
                    onClose = onClose,
                    pageConfig = { page ->
                        when (page) {
                            is ImportFlowPage.Selector -> BottomSheetPageConfig(
                                showBackButton = false,
                                properties = BottomSheetProperties(),
                            )
                            is ImportFlowPage.Manual -> BottomSheetPageConfig(
                                properties = LockedSheetProperties,
                            )
                            is ImportFlowPage.AiAssistant -> BottomSheetPageConfig(
                                showBackButton = false,
                                showCloseButton = false,
                                properties = LockedSheetProperties,
                            )
                        }
                    },
                ) { currentPage ->
                    when (currentPage) {
                        is ImportFlowPage.Selector -> ImportMethodSelectorContent(
                            onManual = { pages.navigateTo(ImportFlowPage.Manual) },
                            onAiAssistant = { pages.navigateTo(ImportFlowPage.AiAssistant) },
                        )
                        is ImportFlowPage.Manual -> ImportBottomSheet(
                            onDismiss = onClose,
                            onShowSnackBar = onImportSuccess,
                        )
                        is ImportFlowPage.AiAssistant -> AiWordImportBottomSheet(
                            onDismiss = onClose,
                            onShowSnackBar = onImportSuccess,
                        )
                    }
                }
            }
        } else {
            overlayHost.showSizeToFitBottomSheet(
                tag = "import",
                properties = LockedSheetProperties,
            ) { nav ->
                ImportBottomSheet(
                    onClose = { nav.dismiss() },
                    onDismiss = { nav.dismiss() },
                    onShowSnackBar = onImportSuccess,
                )
            }
        }
    }

    LexiconColumn(
        title = null,
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Person,
        navigationIconContentDescription = stringResource(Res.string.profile),
        onNavigationClick = { overlayHost.showProfileSheet(snackbarHostState) },
        scrollState = scrollState,
        collapsedContent = {
            CollapsedStatsBar(
                visible = isStatsSectionScrolledAway && progressStats != null,
                stats = progressStats ?: return@LexiconColumn,
            )
        },
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Insights,
            contentDescription = stringResource(Res.string.insights_title),
            onClick = { overlayHost.showInsightsSheet(onShowLeaderboard = { overlayHost.showLeaderboard() }) },
            size = Theme.dimensions.iconSize,
        ),
        actionIcon2 = ActionIconConfig(
            icon = Icons.Default.Add,
            contentDescription = stringResource(Res.string.import_words),
            onClick = openImportSheet,
            size = Theme.dimensions.iconSize,
        ),
        scrollable = true,
    ) {
        Column(Modifier.padding(bottom = Theme.spacing.xs)) {
            when (uiState) {
                is UiState.Loading -> {
                    LoadingScreen(message = "Preparing your study session...")
                }

                is UiState.Error -> {
                    val errorMessage = uiState.message
                    val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                        errorMessage.contains("connect", ignoreCase = true) ||
                        errorMessage.contains("network", ignoreCase = true) ||
                        errorMessage.contains("internet", ignoreCase = true)

                    ErrorScreen(
                        message = if (isNetworkError) {
                            "You're offline -- changes will sync when reconnected."
                        } else {
                            errorMessage.ifEmpty { "Something went wrong loading your progress." }
                        },
                        title = if (isNetworkError) "No Connection" else "Oops!",
                        icon = if (isNetworkError) Icons.Default.WifiOff else null,
                        retryLabel = "Try Again",
                        onRetry = { progressViewModel.refreshStats() },
                    )
                }

                is UiState.Loaded -> {
                    val loadedState = uiState.value
                    val loadedStats = loadedState.progressStats
                    val evaluation = loadedState.progressEvaluation

                    StatsSection(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            statsSectionBottom =
                                (coordinates.positionInParent().y + coordinates.size.height).toInt()
                        },
                        evaluation = evaluation,
                        dueCards = loadedStats.dueCards,
                        onImportWords = openImportSheet,
                        onStartReviewLongPress = { openReviewScreen(ReviewSource.DueCards) },
                        onStartReview = {
                            if (dueTags.isNotEmpty() && !skipTagSelector) {
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "review-selector",
                                    properties = BottomSheetProperties(
                                        dismissOnBackPress = true,
                                        dismissOnTouchOutside = true,
                                    ),
                                ) { nav ->
                                    val sheetProgressState by progressViewModel.state()
                                    ReviewSelectorSheetContent(
                                        title = "Start Review",
                                        allLabel = "All due words",
                                        allCount = loadedStats.dueCards,
                                        tags = dueTags,
                                        skipTagSelector = sheetProgressState.skipTagSelector,
                                        onSkipTagSelectorChanged = { progressViewModel.setSkipTagSelector(it) },
                                        onAllSelected = {
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.DueCards)
                                        },
                                        onTagSelected = { tag ->
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.ByTag(tag.id))
                                        },
                                    )
                                }
                            } else {
                                openReviewScreen(ReviewSource.DueCards)
                            }
                        },
                    )

                    WordDistributionBar(stats = loadedStats)

                    // Do NOT use `by` here — `wordRushViewModel.state()` updates every 50 ms
                    // (timer tick) and would cause StudyScreen to recompose at 20 fps.
                    // derivedStateOf ensures StudyScreen only recomposes when bestStreak or
                    // hasEnoughWords actually changes (rare), not on every timer tick.
                    val wordRushStateHolder = wordRushViewModel.state()
                    val wordRushBestStreak by remember { derivedStateOf { wordRushStateHolder.value.bestStreak } }
                    val wordRushHasEnoughWords by remember {
                        derivedStateOf { wordRushStateHolder.value.hasEnoughWords }
                    }
                    WordRushCard(
                        bestStreak = wordRushBestStreak,
                        hasEnoughWords = wordRushHasEnoughWords,
                        onPlay = {
                            wordRushViewModel.startGame()
                            overlayHost.showFullScreen(
                                tag = "word-rush",
                                properties = FullScreenProperties(
                                    dismissOnBackPress = false,
                                    isNavigationBarsPaddingEnabled = true,
                                ),
                            ) { navigator ->
                                // No `by` — stateHolder is passed directly to WordRushGameScreen,
                                // which uses derivedStateOf internally. This overlay composable
                                // itself does not recompose on every 50 ms timer tick.
                                val gameStateHolder = wordRushViewModel.state()
                                OnEvents(wordRushViewModel.effects) { effect ->
                                    when (effect) {
                                        WordRushEffect.GameComplete -> {
                                            progressViewModel.refreshStats()
                                        }
                                    }
                                }
                                WordRushGameScreen(
                                    stateHolder = gameStateHolder,
                                    onSelectAnswer = wordRushViewModel::selectAnswer,
                                    onUsePowerUp = wordRushViewModel::usePowerUp,
                                    onPlayAgain = wordRushViewModel::startGame,
                                    onDismiss = {
                                        wordRushViewModel.dismiss()
                                        navigator.dismiss()
                                    },
                                )
                            }
                        },
                        modifier = Modifier.padding(vertical = Theme.spacing.sm),
                    )

                    LearningStagesSection(
                        stats = loadedStats,
                        onStageLongClick = { stage, _ ->
                            openReviewScreen(ReviewSource.ByStage(stage))
                        },
                        onStageClick = { stage, stageName ->
                            val stageTags = stageTagsMap[stage.ordinal].orEmpty()
                            if (stageTags.isNotEmpty() && !skipTagSelector) {
                                val stageCount = when (stage) {
                                    LearningStage.LEVEL_0_FRESH -> loadedStats.level0Count
                                    LearningStage.LEVEL_1_LEARNING -> loadedStats.level1Count
                                    LearningStage.LEVEL_2_FAMILIAR -> loadedStats.level2Count
                                    LearningStage.LEVEL_3_BUILDING -> loadedStats.level3Count
                                    LearningStage.LEVEL_4_ALMOST -> loadedStats.level4Count
                                    LearningStage.LEVEL_5_STRONG -> loadedStats.level5Count
                                    LearningStage.LEVEL_6_MASTERED -> loadedStats.level6Count
                                }
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "stage-selector-${stage}",
                                    properties = BottomSheetProperties(
                                        dismissOnBackPress = true,
                                        dismissOnTouchOutside = true,
                                    ),
                                ) { nav ->
                                    val sheetProgressState by progressViewModel.state()
                                    ReviewSelectorSheetContent(
                                        title = stageName,
                                        allLabel = "All $stageName",
                                        allCount = stageCount,
                                        tags = stageTags,
                                        skipTagSelector = sheetProgressState.skipTagSelector,
                                        onSkipTagSelectorChanged = { progressViewModel.setSkipTagSelector(it) },
                                        onAllSelected = {
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.ByStage(stage))
                                        },
                                        onTagSelected = { tag ->
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.ByStageAndTag(stage, tag.id))
                                        },
                                    )
                                }
                            } else {
                                openReviewScreen(ReviewSource.ByStage(stage))
                            }
                        },
                    )
                }
            }
        }
    }
}


@Composable
private fun ReviewSelectorSheetContent(
    title: String,
    allLabel: String,
    allCount: Int,
    tags: List<Tag>,
    skipTagSelector: Boolean,
    onSkipTagSelectorChanged: (Boolean) -> Unit,
    onAllSelected: () -> Unit,
    onTagSelected: (Tag) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Theme.spacing.md)) {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(vertical = Theme.spacing.md),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            LevelBucketCard(
                level = allLabel,
                description = "All words",
                count = allCount,
                color = AppColors.secondary,
                icon = Icons.Rounded.MenuBook,
                onClick = onAllSelected,
            )
            tags.forEach { tag ->
                LevelBucketCard(
                    level = tag.name,
                    description = "${tag.wordCount} ${if (tag.wordCount == 1L) "word" else "words"}",
                    count = tag.wordCount.toInt(),
                    color = AppColors.adept,
                    icon = Icons.Rounded.Sell,
                    onClick = { onTagSelected(tag) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = skipTagSelector,
                onCheckedChange = onSkipTagSelectorChanged,
            )
            Text(
                text = stringResource(Res.string.skip_tag_selector_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(Theme.spacing.xs))
    }
}
