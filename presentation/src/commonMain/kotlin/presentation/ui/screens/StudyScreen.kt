package presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheetDefaults.properties
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import feature.study.StudyEvent
import feature.study.StudyViewModel
import feature.study.model.ReviewType
import core.common.UiState
import presentation.ui.LocalSnackbarHostState
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import presentation.ui.components.imports.AiWordImportBottomSheet
import presentation.ui.components.imports.ImportBottomSheet
import presentation.ui.components.imports.ImportMethodSelectorContent
import overlay.LocalOverlayHost
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import feature.study.ui.review.ReviewBottomSheetContent
import feature.study.ui.study.CollapsedStatsBar
import feature.study.ui.study.LearningStagesSection
import feature.study.ui.study.StatsSection
import feature.study.ui.study.WordDistributionBar
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.review_due_cards
import lexicon.resources.generated.resources.stage_words_string
import overlay.bottomsheet.showSizeToFitBottomSheet

/** Non-dismissable sheet configuration reused for import and review flows. */
private val LockedSheetProperties = BottomSheetProperties(
    dismissOnTouchOutside = false,
    dismissOnBackPress = false,
    isNavigationBarsPaddingEnabled = true,
    sheetGesturesEnabled = false,
)

private sealed interface ImportFlowPage {
    data object Selector : ImportFlowPage
    data object Manual : ImportFlowPage
    data object AiAssistant : ImportFlowPage
}

@Composable
fun StudyScreen() {
    val viewModel = koinViewModel<StudyViewModel>()
    val overlayHost = LocalOverlayHost.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val screenState by viewModel.state()
    val uiState = screenState.progress
    val hasPremiumAccess = screenState.hasPremiumAccess

    val scrollState = rememberScrollState()
    var statsSectionBottom by remember { mutableIntStateOf(0) }
    val isStatsSectionScrolledAway by remember {
        derivedStateOf { scrollState.value > statsSectionBottom && statsSectionBottom > 0 }
    }

    val progressStats = (uiState as? UiState.Loaded)?.value?.progressStats

    val onImportSuccess: (String) -> Unit = { message ->
        viewModel.refreshStats()
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    val openImportSheet: () -> Unit = {
        if (hasPremiumAccess) {
            overlayHost.showSizeToFitBottomSheet(tag = "import") { sheetNav ->
                val pages = rememberBottomSheetPageNavigator<ImportFlowPage>(ImportFlowPage.Selector)

                LaunchedEffect(pages.currentPage) {
                    properties = when (pages.currentPage) {
                        is ImportFlowPage.Selector -> BottomSheetProperties(showCloseButton = false)
                        is ImportFlowPage.Manual -> LockedSheetProperties.copy(showCloseButton = true)
                        is ImportFlowPage.AiAssistant -> LockedSheetProperties
                    }
                }

                BottomSheetPages(navigator = pages) { currentPage ->
                    when (currentPage) {
                        is ImportFlowPage.Selector -> ImportMethodSelectorContent(
                            onManual = { pages.navigateTo(ImportFlowPage.Manual) },
                            onAiAssistant = { pages.navigateTo(ImportFlowPage.AiAssistant) }
                        )
                        is ImportFlowPage.Manual -> ImportBottomSheet(
                            onDismiss = { sheetNav.dismiss() },
                            onShowSnackBar = onImportSuccess
                        )
                        is ImportFlowPage.AiAssistant -> AiWordImportBottomSheet(
                            onDismiss = { sheetNav.dismiss() },
                            onShowSnackBar = onImportSuccess
                        )
                    }
                }
            }
        } else {
            overlayHost.showSizeToFitBottomSheet(
                tag = "import",
                properties = LockedSheetProperties.copy(showCloseButton = true)
            ) { nav ->
                ImportBottomSheet(
                    onDismiss = { nav.dismiss() },
                    onShowSnackBar = onImportSuccess
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    // Error state handled by snackbar if needed
                }

                is UiState.Loaded -> {
                    val loadedState = (uiState as UiState.Loaded).value
                    val loadedStats = loadedState.progressStats
                    val evaluation = loadedState.progressEvaluation

                    OnEvents(viewModel.effects) { event ->
                        when (event) {
                            is StudyEvent.StartReview -> {
                                viewModel.startDueReview()
                                overlayHost.showFullScreen(
                                    tag = "review-due",
                                    properties = FullScreenProperties(
                                        dismissOnBackPress = false,
                                        isNavigationBarsPaddingEnabled = true,
                                    )
                                ) { navigator ->
                                    // Read state inside the composable lambda so the
                                    // overlay host recomposes when the ViewModel updates.
                                    val sheetState by viewModel.state()
                                    val sheetTts = sheetState.ttsState

                                    ReviewBottomSheetContent(
                                        title = stringResource(Res.string.review_due_cards),
                                        reviewType = ReviewType.REVIEW,
                                        reviewState = sheetState.review,
                                        initialWord = event.firstWord,
                                        onClose = { navigator.dismiss() },
                                        onReviewComplete = {
                                            viewModel.onReviewSessionComplete()
                                            navigator.dismiss()
                                        },
                                        onReviewWord = viewModel::reviewWord,
                                        onLoadWords = viewModel::loadWords,
                                        onUpdateWord = viewModel::updateWord,
                                        onDeleteWord = { wordId, onComplete ->
                                            viewModel.deleteWord(wordId)
                                            onComplete()
                                        },
                                        ttsState = sheetTts,
                                        onSpeakClick = viewModel::speakWord
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
                        onStartReview = { viewModel.startReview() }
                    )

                    WordDistributionBar(stats = loadedStats)

                    LearningStagesSection(
                        stats = loadedStats,
                        onStageClick = { stage, stageName ->
                            viewModel.loadWordsByStage(stage)
                            overlayHost.showFullScreen(tag = "review-stage-${stage}") { navigator ->
                                val sheetState by viewModel.state()
                                val sheetTts = sheetState.ttsState

                                ReviewBottomSheetContent(
                                    title = stringResource(Res.string.stage_words_string, stageName),
                                    reviewType = ReviewType.BROWSE,
                                    reviewState = sheetState.review,
                                    onClose = navigator::dismiss,
                                    onReviewComplete = navigator::dismiss,
                                    onReviewWord = viewModel::reviewWord,
                                    onLoadWords = viewModel::loadWords,
                                    onUpdateWord = viewModel::updateWord,
                                    onDeleteWord = { wordId, onComplete ->
                                        viewModel.deleteWord(wordId)
                                        onComplete()
                                    },
                                    ttsState = sheetTts,
                                    onSpeakClick = viewModel::speakWord
                                )
                            }
                            viewModel.startStageReview(stage)
                        }
                    )
                }
            }
        }
    }
}
