package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Sell
import domain.word.model.LearningStage
import components.SectionHeader
import domain.tag.model.Tag
import theme.AppColors
import androidx.compose.material3.SnackbarDuration
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
import feature.study.ui.components.LevelBucketCard
import feature.study.ui.review.ReviewBottomSheetContent
import feature.study.ui.study.CollapsedStatsBar
import feature.study.ui.study.LearningStagesSection
import feature.study.ui.study.StatsSection
import feature.insights.navigation.showInsightsSheet
import feature.study.ui.study.WordDistributionBar
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.insights_title
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
    val dueTags = progressState.dueTags
    val skipTagSelector = progressState.skipTagSelector
    val stageTagsMap = progressState.stageTagsMap

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
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Insights,
            contentDescription = stringResource(Res.string.insights_title),
            onClick = { overlayHost.showInsightsSheet() },
            size = Theme.dimensions.iconSize
        ),
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
                        onRetry = { progressViewModel.refreshStats() }
                    )
                }

                is UiState.Loaded -> {
                    val loadedState = uiState.value
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
                                        onLoadWords = reviewViewModel::startDueReview,
                                        onUpdateWord = reviewViewModel::updateWord,
                                        onDeleteWord = { wordId, onComplete ->
                                            reviewViewModel.deleteWord(wordId)
                                            onComplete()
                                        },
                                        ttsState = sheetTts,
                                        onSpeakClick = reviewViewModel::speakWord,
                                        speechRate = reviewState.speechRate,
                                        onSpeechRateChanged = reviewViewModel::setTtsSpeechRate,
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
                        onStartReviewLongPress = { reviewViewModel.startReview() },
                        onStartReview = {
                            if (dueTags.isNotEmpty() && !skipTagSelector) {
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "review-selector",
                                    properties = BottomSheetProperties(dismissOnBackPress = true, dismissOnTouchOutside = true),
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
                                            reviewViewModel.startReview()
                                        },
                                        onTagSelected = { tag ->
                                            nav.dismiss()
                                            reviewViewModel.startTagReview(tag.id)
                                            overlayHost.showFullScreen(
                                                tag = "review-tag-${tag.id}",
                                                properties = FullScreenProperties(
                                                    dismissOnBackPress = false,
                                                    isNavigationBarsPaddingEnabled = true
                                                ),
                                            ) { navigator ->
                                                val reviewState by reviewViewModel.state()
                                                ReviewBottomSheetContent(
                                                    reviewType = ReviewType.REVIEW,
                                                    reviewState = reviewState.review,
                                                    onClose = navigator::dismiss,
                                                    onReviewComplete = {
                                                        reviewViewModel.onReviewSessionComplete()
                                                        navigator.dismiss()
                                                    },
                                                    onReviewWord = reviewViewModel::reviewWord,
                                                    onLoadWords = reviewViewModel::startDueReview,
                                                    onUpdateWord = reviewViewModel::updateWord,
                                                    onDeleteWord = { wordId, onComplete ->
                                                        reviewViewModel.deleteWord(wordId)
                                                        onComplete()
                                                    },
                                                    ttsState = reviewState.ttsState,
                                                    onSpeakClick = reviewViewModel::speakWord,
                                                    speechRate = reviewState.speechRate,
                                                    onSpeechRateChanged = reviewViewModel::setTtsSpeechRate,
                                                )
                                            }
                                        },
                                    )
                                }
                            } else {
                                reviewViewModel.startReview()
                            }
                        },
                    )

                    WordDistributionBar(stats = loadedStats)

                    LearningStagesSection(
                        stats = loadedStats,
                        onStageLongClick = { stage, _ ->
                            reviewViewModel.loadWordsByStage(stage)
                            overlayHost.showFullScreen(
                                tag = "review-stage-${stage}",
                                properties = FullScreenProperties(dismissOnSwipe = true),
                            ) { navigator ->
                                val reviewState by reviewViewModel.state()
                                ReviewBottomSheetContent(
                                    reviewType = ReviewType.BROWSE,
                                    reviewState = reviewState.review,
                                    onClose = navigator::dismiss,
                                    onReviewComplete = {
                                        reviewViewModel.onReviewSessionComplete()
                                        navigator.dismiss()
                                    },
                                    onReviewWord = reviewViewModel::reviewWord,
                                    onLoadWords = reviewViewModel::startDueReview,
                                    onUpdateWord = reviewViewModel::updateWord,
                                    onDeleteWord = { wordId, onComplete ->
                                        reviewViewModel.deleteWord(wordId)
                                        onComplete()
                                    },
                                    ttsState = reviewState.ttsState,
                                    onSpeakClick = reviewViewModel::speakWord,
                                    speechRate = reviewState.speechRate,
                                    onSpeechRateChanged = reviewViewModel::setTtsSpeechRate,
                                )
                            }
                            reviewViewModel.startStageReview(stage)
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
                                    properties = BottomSheetProperties(dismissOnBackPress = true, dismissOnTouchOutside = true),
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
                                            reviewViewModel.loadWordsByStage(stage)
                                            overlayHost.showFullScreen(
                                                tag = "review-stage-${stage}",
                                                properties = FullScreenProperties(dismissOnSwipe = true),
                                            ) { navigator ->
                                                val reviewState by reviewViewModel.state()
                                                ReviewBottomSheetContent(
                                                    reviewType = ReviewType.BROWSE,
                                                    reviewState = reviewState.review,
                                                    onClose = navigator::dismiss,
                                                    onReviewComplete = {
                                                        reviewViewModel.onReviewSessionComplete()
                                                        navigator.dismiss()
                                                    },
                                                    onReviewWord = reviewViewModel::reviewWord,
                                                    onLoadWords = reviewViewModel::startDueReview,
                                                    onUpdateWord = reviewViewModel::updateWord,
                                                    onDeleteWord = { wordId, onComplete ->
                                                        reviewViewModel.deleteWord(wordId)
                                                        onComplete()
                                                    },
                                                    ttsState = reviewState.ttsState,
                                                    onSpeakClick = reviewViewModel::speakWord,
                                                    speechRate = reviewState.speechRate,
                                                    onSpeechRateChanged = reviewViewModel::setTtsSpeechRate,
                                                )
                                            }
                                            reviewViewModel.startStageReview(stage)
                                        },
                                        onTagSelected = { tag ->
                                            nav.dismiss()
                                            reviewViewModel.startStageTagReview(stage, tag.id)
                                            overlayHost.showFullScreen(
                                                tag = "review-stage-tag-${stage}-${tag.id}",
                                                properties = FullScreenProperties(dismissOnSwipe = true),
                                            ) { navigator ->
                                                val reviewState by reviewViewModel.state()
                                                ReviewBottomSheetContent(
                                                    reviewType = ReviewType.BROWSE,
                                                    reviewState = reviewState.review,
                                                    onClose = navigator::dismiss,
                                                    onReviewComplete = {
                                                        reviewViewModel.onReviewSessionComplete()
                                                        navigator.dismiss()
                                                    },
                                                    onReviewWord = reviewViewModel::reviewWord,
                                                    onLoadWords = reviewViewModel::startDueReview,
                                                    onUpdateWord = reviewViewModel::updateWord,
                                                    onDeleteWord = { wordId, onComplete ->
                                                        reviewViewModel.deleteWord(wordId)
                                                        onComplete()
                                                    },
                                                    ttsState = reviewState.ttsState,
                                                    onSpeakClick = reviewViewModel::speakWord,
                                                    speechRate = reviewState.speechRate,
                                                    onSpeechRateChanged = reviewViewModel::setTtsSpeechRate,
                                                )
                                            }
                                        },
                                    )
                                }
                            } else {
                                reviewViewModel.loadWordsByStage(stage)
                                overlayHost.showFullScreen(
                                    tag = "review-stage-${stage}",
                                    properties = FullScreenProperties(dismissOnSwipe = true),
                                ) { navigator ->
                                    val reviewState by reviewViewModel.state()
                                    ReviewBottomSheetContent(
                                        reviewType = ReviewType.BROWSE,
                                        reviewState = reviewState.review,
                                        onClose = navigator::dismiss,
                                        onReviewComplete = {
                                            reviewViewModel.onReviewSessionComplete()
                                            navigator.dismiss()
                                        },
                                        onReviewWord = reviewViewModel::reviewWord,
                                        onLoadWords = reviewViewModel::startDueReview,
                                        onUpdateWord = reviewViewModel::updateWord,
                                        onDeleteWord = { wordId, onComplete ->
                                            reviewViewModel.deleteWord(wordId)
                                            onComplete()
                                        },
                                        ttsState = reviewState.ttsState,
                                        onSpeakClick = reviewViewModel::speakWord,
                                        speechRate = reviewState.speechRate,
                                        onSpeechRateChanged = reviewViewModel::setTtsSpeechRate,
                                    )
                                }
                                reviewViewModel.startStageReview(stage)
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
            Column {
                Text(
                    text = "Don't ask again, always review all words",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(Theme.spacing.xs))
    }
}
