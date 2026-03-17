package presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import events.OnEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.study.ReviewEffect
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.model.ReviewType
import core.common.UiState
import components.ErrorScreen
import components.LoadingScreen
import presentation.ui.LocalSnackbarHostState
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import presentation.ui.components.imports.AiWordImportBottomSheet
import presentation.ui.components.imports.ImportBottomSheet
import presentation.ui.components.imports.ImportMethodSelectorContent
import overlay.LocalOverlayHost
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import feature.study.ui.review.ReviewBottomSheetContent
import feature.study.ui.study.CollapsedStatsBar
import feature.study.ui.study.LearningStagesSection
import feature.study.ui.study.StatsSection
import feature.insights.navigation.showInsightsSheet
import feature.study.model.WeeklyReportUiModel
import feature.study.ui.study.WeeklyReportCard
import feature.study.ui.study.WordDistributionBar
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_words
import overlay.bottomsheet.showSizeToFitBottomSheet

/** Non-dismissable sheet configuration reused for import and review flows. */
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
    val overlayHost = LocalOverlayHost.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val progressState by progressViewModel.state()
    val uiState = progressState.progress
    val hasPremiumAccess = progressState.hasPremiumAccess

    val scrollState = rememberScrollState()
    var statsSectionBottom by remember { mutableIntStateOf(0) }
    val isStatsSectionScrolledAway by remember {
        derivedStateOf { scrollState.value > statsSectionBottom && statsSectionBottom > 0 }
    }

    val progressStats = (uiState as? UiState.Loaded)?.value?.progressStats

    val onImportSuccess: (String) -> Unit = { message ->
        progressViewModel.refreshStats()
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    val openImportSheet: () -> Unit = {
        if (hasPremiumAccess) {
            overlayHost.showSizeToFitBottomSheet(
                tag = "import",
                properties = BottomSheetProperties(dismissOnBackPress = true, dismissOnTouchOutside = true)
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
                            onAiAssistant = { pages.navigateTo(ImportFlowPage.AiAssistant) }
                        )

                        is ImportFlowPage.Manual -> ImportBottomSheet(
                            onDismiss = onClose,
                            onShowSnackBar = onImportSuccess
                        )

                        is ImportFlowPage.AiAssistant -> AiWordImportBottomSheet(
                            onDismiss = onClose,
                            onShowSnackBar = onImportSuccess
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
        showNavigationIcon = false,
        scrollState = scrollState,
        collapsedContent = {
            CollapsedStatsBar(
                visible = isStatsSectionScrolledAway && progressStats != null,
                stats = progressStats ?: return@LexiconColumn,
            )
        },
        actionIcon2 = ActionIconConfig(
            icon = Icons.Default.Add,
            contentDescription = stringResource(Res.string.import_words),
            onClick = openImportSheet,
            size = Theme.dimensions.iconSize
        ),
        scrollable = true,
    ) {
        Column(Modifier.padding(bottom = Theme.spacing.xs)) {
            when (uiState) {
                is UiState.Loading -> {
                    LoadingScreen(message = "Preparing your study session...")
                }

                is UiState.Error -> {
                    val errorMessage = (uiState as UiState.Error).message
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
                        onRetry = { progressViewModel.refreshStats() }
                    )
                }

                is UiState.Loaded -> {
                    val loadedState = (uiState as UiState.Loaded).value
                    val loadedStats = loadedState.progressStats
                    val evaluation = loadedState.progressEvaluation

                    OnEvents(reviewViewModel.effects) { effect ->
                        when (effect) {
                            is ReviewEffect.StartReview -> {
                                reviewViewModel.startDueReview()
                                overlayHost.showFullScreen(
                                    tag = "review-due",
                                    properties = FullScreenProperties(
                                        dismissOnBackPress = false,
                                        isNavigationBarsPaddingEnabled = true,
                                    )
                                ) { navigator ->
                                    val reviewState by reviewViewModel.state()
                                    val sheetTts = reviewState.ttsState

                                    ReviewBottomSheetContent(
                                        reviewType = ReviewType.REVIEW,
                                        reviewState = reviewState.review,
                                        initialWord = effect.firstWord,
                                        onClose = { navigator.dismiss() },
                                        onReviewComplete = {
                                            reviewViewModel.onReviewSessionComplete()
                                            navigator.dismiss()
                                        },
                                        onReviewWord = reviewViewModel::reviewWord,
                                        onLoadWords = reviewViewModel::loadWords,
                                        onUpdateWord = reviewViewModel::updateWord,
                                        onDeleteWord = { wordId, onComplete ->
                                            reviewViewModel.deleteWord(wordId)
                                            onComplete()
                                        },
                                        ttsState = sheetTts,
                                        onSpeakClick = reviewViewModel::speakWord
                                    )
                                }
                            }
                        }
                    }

                    StatsSection(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            statsSectionBottom =
                                (coordinates.positionInParent().y + coordinates.size.height).toInt()
                        },
                        evaluation = evaluation,
                        dueCards = loadedStats.dueCards,
                        onImportWords = openImportSheet,
                        onStartReview = { reviewViewModel.startReview() }
                    )

                    val weeklyReport = (progressState.weeklyReport as? UiState.Loaded)?.value
                    if (weeklyReport is WeeklyReportUiModel.Content) {
                        Spacer(Modifier.height(Theme.spacing.sm))
                        WeeklyReportCard(
                            report = weeklyReport,
                            onViewInsights = {
                                overlayHost.showInsightsSheet()
                            },
                        )
                    }

                    WordDistributionBar(stats = loadedStats)

                    LearningStagesSection(
                        stats = loadedStats,
                        onStageClick = { stage, stageName ->
                            reviewViewModel.loadWordsByStage(stage)
                            overlayHost.showFullScreen(tag = "review-stage-${stage}") { navigator ->
                                val reviewState by reviewViewModel.state()
                                val sheetTts = reviewState.ttsState

                                ReviewBottomSheetContent(
                                    reviewType = ReviewType.BROWSE,
                                    reviewState = reviewState.review,
                                    onClose = navigator::dismiss,
                                    onReviewComplete = navigator::dismiss,
                                    onReviewWord = reviewViewModel::reviewWord,
                                    onLoadWords = reviewViewModel::loadWords,
                                    onUpdateWord = reviewViewModel::updateWord,
                                    onDeleteWord = { wordId, onComplete ->
                                        reviewViewModel.deleteWord(wordId)
                                        onComplete()
                                    },
                                    ttsState = sheetTts,
                                    onSpeakClick = reviewViewModel::speakWord
                                )
                            }
                            reviewViewModel.startStageReview(stage)
                        }
                    )
                }
            }
        }
    }
}
