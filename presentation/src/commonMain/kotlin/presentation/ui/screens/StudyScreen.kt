package presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import events.OnEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.study.StudyEvent
import presentation.feature.study.StudyViewModel
import presentation.model.ReviewType
import presentation.model.UiState
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.ActionIconConfig
import presentation.ui.components.CloseConfirmationDialogContent
import presentation.ui.components.LexiconColumn
import presentation.ui.components.imports.AiWordImportBottomSheet
import presentation.ui.components.imports.ImportBottomSheet
import presentation.ui.components.imports.ImportMethodSelectorContent
import presentation.ui.overlay.LocalOverlayHost
import presentation.ui.overlay.bottomsheet.BottomSheetProperties
import presentation.ui.overlay.bottomsheet.showFullscreenBottomSheet
import presentation.ui.overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.overlay.dialog.showDialog
import presentation.ui.screens.review.ReviewBottomSheetContent
import presentation.ui.screens.study.CollapsedStatsBar
import presentation.ui.screens.study.LearningStagesSection
import presentation.ui.screens.study.StatsSection
import presentation.ui.screens.study.WordDistributionBar
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.review_due_cards
import lexicon.resources.generated.resources.stage_words_string

/** Non-dismissable sheet configuration reused for import and review flows. */
private val LockedSheetProperties = BottomSheetProperties(
    dismissOnTouchOutside = false,
    dismissOnBackPress = false,
    isNavigationBarsPaddingEnabled = true,
    sheetGesturesEnabled = false,
)

@Composable
fun StudyScreen() {
    val viewModel = koinViewModel<StudyViewModel>()
    val overlayHost = LocalOverlayHost.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.progressScreenState.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewScreenState.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val hasPremiumAccess by viewModel.hasPremiumAccess.collectAsStateWithLifecycle(false)

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
            overlayHost.showSizeToFitBottomSheet(tag = "import-method") { selectorNav ->
                ImportMethodSelectorContent(
                    onManual = {
                        selectorNav.dismiss()
                        overlayHost.showFullscreenBottomSheet(
                            tag = "import",
                            properties = LockedSheetProperties
                        ) { nav ->
                            ImportBottomSheet(
                                onDismiss = { nav.dismiss() },
                                onShowSnackBar = onImportSuccess
                            )
                        }
                    },
                    onAiAssistant = {
                        selectorNav.dismiss()
                        overlayHost.showFullscreenBottomSheet(
                            tag = "ai-import",
                            properties = LockedSheetProperties
                        ) { nav ->
                            AiWordImportBottomSheet(
                                onDismiss = { nav.dismiss() },
                                onShowSnackBar = onImportSuccess
                            )
                        }
                    }
                )
            }
        } else {
            overlayHost.showFullscreenBottomSheet(
                tag = "import",
                properties = LockedSheetProperties
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

                    OnEvents(viewModel.events) { event ->
                        when (event) {
                            is StudyEvent.StartReview -> {
                                viewModel.startDueReview()
                                overlayHost.showFullscreenBottomSheet(
                                    tag = "review-due",
                                    properties = LockedSheetProperties
                                ) { navigator ->
                                    ReviewBottomSheetContent(
                                        title = stringResource(Res.string.review_due_cards),
                                        reviewType = ReviewType.REVIEW,
                                        reviewState = reviewState,
                                        initialWord = event.firstWord,
                                        onClose = {
                                            overlayHost.showDialog(tag = "exit-confirmation") { nav ->
                                                CloseConfirmationDialogContent(
                                                    onConfirm = {
                                                        nav.dismiss()
                                                        navigator.dismiss()
                                                    },
                                                    onDismiss = nav::dismiss
                                                )
                                            }
                                        },
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
                                        ttsState = ttsState,
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
                            overlayHost.showFullscreenBottomSheet(tag = "review-stage-${stage}") { navigator ->
                                ReviewBottomSheetContent(
                                    title = stringResource(Res.string.stage_words_string, stageName),
                                    reviewType = ReviewType.BROWSE,
                                    reviewState = reviewState,
                                    onClose = navigator::dismiss,
                                    onReviewComplete = navigator::dismiss,
                                    onReviewWord = viewModel::reviewWord,
                                    onLoadWords = viewModel::loadWords,
                                    onUpdateWord = viewModel::updateWord,
                                    onDeleteWord = { wordId, onComplete ->
                                        viewModel.deleteWord(wordId)
                                        onComplete()
                                    },
                                    ttsState = ttsState,
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
