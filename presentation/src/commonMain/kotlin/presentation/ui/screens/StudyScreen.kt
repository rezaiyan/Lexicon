package presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import events.OnEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.study.StudyEvent
import presentation.feature.study.StudyViewModel
import presentation.model.UiState
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.ActionIconConfig
import presentation.ui.components.CloseConfirmationDialogContent
import presentation.ui.components.LexiconColumn
import presentation.ui.components.imports.ImportBottomSheet
import presentation.ui.overlay.LocalOverlayHost
import presentation.ui.overlay.bottomsheet.BottomSheetProperties
import presentation.ui.overlay.bottomsheet.showFullscreenBottomSheet
import presentation.ui.overlay.dialog.showDialog
import presentation.ui.screens.review.ReviewBottomSheetContent
import presentation.ui.screens.study.LearningStagesSection
import presentation.ui.screens.study.StatsSection
import presentation.utils.getTimeBasedGreeting
import presentation.viewmodel.VocabularyViewModel
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.import_words
import vokab.resources.generated.resources.review_due_cards
import vokab.resources.generated.resources.stage_words_string

@Composable
fun StudyScreen() {
    val vocabularyViewModel = koinViewModel<VocabularyViewModel>()
    val viewModel = koinViewModel<StudyViewModel>()
    val overlayHost = LocalOverlayHost.current

    val uiState by viewModel.progressScreenState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    LexiconColumn(
        title = getTimeBasedGreeting(),
        showNavigationIcon = false,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Add,
            contentDescription = stringResource(Res.string.import_words),
            onClick = {
                overlayHost.showFullscreenBottomSheet(
                    tag = "import",
                    properties = BottomSheetProperties(
                        dismissOnTouchOutside = false,
                        dismissOnBackPress = false,
                        isNavigationBarsPaddingEnabled = true,
                        sheetGesturesEnabled = false,
                    )
                ) { navigator ->
                    ImportBottomSheet(
                        onDismiss = {
                            navigator.dismiss()
                        },
                        onShowSnackBar = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }

                        }
                    )
                }
            },
            size = 24.dp
        ),
        scrollable = true,
    ) {
        Column {
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
                    val progressStats = (uiState as UiState.Loaded).value.progressStats

                    OnEvents(viewModel.events) { event ->
                        when (event) {
                            is StudyEvent.StartReview -> {
                                vocabularyViewModel.startDueReview()
                                overlayHost.showFullscreenBottomSheet(
                                    tag = "review-due",
                                    properties = BottomSheetProperties(
                                        dismissOnTouchOutside = false,
                                        dismissOnBackPress = false,
                                        isNavigationBarsPaddingEnabled = true,
                                        sheetGesturesEnabled = false,
                                    )
                                ) { navigator ->
                                    ReviewBottomSheetContent(
                                        title = stringResource(Res.string.review_due_cards),
                                        reviewType = presentation.model.ReviewType.REVIEW,
                                        initialWord = event.firstWord,
                                        vocabularyViewModel = vocabularyViewModel,
                                        onClose = {
                                            overlayHost.showDialog(tag = "exit-confirmation") { nav ->
                                                CloseConfirmationDialogContent(
                                                    onConfirm = {
                                                        nav.dismiss()
                                                        navigator.dismiss()
                                                    },
                                                    onDismiss = { nav.dismiss() }
                                                )
                                            }
                                        },
                                        onReviewComplete = {
                                            vocabularyViewModel.onReviewSessionComplete()
                                            navigator.dismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    StatsSection(
                        stats = progressStats,
                        onStartReview = {
                            viewModel.startReview()
                        }
                    )

                    LearningStagesSection(
                        stats = progressStats,
                        onStageClick = { stage, stageName ->
                            vocabularyViewModel.loadWordsByStage(stage)
                            overlayHost.showFullscreenBottomSheet(tag = "review-stage-${stage}") { navigator ->
                                ReviewBottomSheetContent(
                                    title = stringResource(Res.string.stage_words_string, stageName),
                                    reviewType = presentation.model.ReviewType.BROWSE,
                                    vocabularyViewModel = vocabularyViewModel,
                                    onClose = { navigator.dismiss() },
                                    onReviewComplete = {
                                        vocabularyViewModel.onReviewSessionComplete()
                                        navigator.dismiss()
                                    }
                                )
                            }
                            vocabularyViewModel.startStageReview(stage)
                        }
                    )
                }
            }
        }
    }
}

