package feature.study.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.DialogIconState
import components.dialog.LexiconDialogContent
import expects.BackHandler
import feature.study.ReviewState
import feature.study.ReviewViewModel
import feature.study.model.ReviewType
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.exit_review
import lexicon.resources.generated.resources.exit_review_message
import org.jetbrains.compose.resources.stringResource
import overlay.LocalOverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet

/**
 * Purely declarative review screen — no mutable `remember` cursors, no
 * `LaunchedEffect` for business logic.  All state lives in [ReviewViewModel].
 *
 * Call [ReviewViewModel.startSession] *before* showing this screen, then
 * call [ReviewViewModel.abandonSession] when the user dismisses without
 * completing.
 */
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state()
    val reviewState = state.review
    val overlayHost = LocalOverlayHost.current

    val exitReviewTitle = stringResource(Res.string.exit_review)
    val exitReviewMessage = stringResource(Res.string.exit_review_message)
    val cancelText = stringResource(Res.string.cancel)

    val showExitConfirmation: () -> Unit = {
        overlayHost.showSizeToFitBottomSheet(tag = "exit-confirmation") { nav ->
            LexiconDialogContent(
                iconState = DialogIconState.Icon(
                    imageVector = Icons.Default.Warning,
                    tint = MaterialTheme.colorScheme.error,
                ),
                title = exitReviewTitle,
                message = exitReviewMessage,
                primaryButton = ButtonState(
                    text = exitReviewTitle,
                    onClick = { nav.dismiss(); onDismiss() },
                    type = ButtonType.Error,
                ),
                secondaryButton = ButtonState(
                    text = cancelText,
                    onClick = { nav.dismiss() },
                ),
            )
        }
    }

    BackHandler(
        enabled = reviewState is ReviewState.Active &&
            reviewState.reviewType == ReviewType.REVIEW,
    ) {
        showExitConfirmation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        when (reviewState) {
            ReviewState.Idle,
            ReviewState.Loading -> LoadingState()

            is ReviewState.Error -> ErrorState(
                error = reviewState.error,
                onRetry = { viewModel.startSession(reviewState.source) },
            )

            is ReviewState.Empty -> EmptyState(reviewState.nextDueAt)

            is ReviewState.Active -> ReviewContent(
                words = reviewState.words,
                currentIndex = reviewState.currentIndex,
                isFlipped = reviewState.isFlipped,
                reviewType = reviewState.reviewType,
                onClose = if (reviewState.reviewType == ReviewType.BROWSE) onDismiss else showExitConfirmation,
                onFlip = viewModel::flipCard,
                onNavigateBack = viewModel::navigateBack,
                onNavigateForward = viewModel::navigateForward,
                onReview = viewModel::reviewWord,
                onEdit = {
                    overlayHost.showSizeToFitBottomSheet(tag = "edit-word") { nav ->
                        EditWordSheetContent(
                            word = reviewState.currentWord,
                            navigator = nav,
                            onSave = viewModel::updateWord,
                            onDelete = { viewModel.deleteWord(reviewState.currentWord.id) },
                        )
                    }
                },
                ttsState = state.ttsState,
                onSpeakClick = viewModel::speakWord,
                isAutoPlayEnabled = reviewState.isAutoPlayEnabled,
                onAutoPlayToggle = viewModel::setAutoPlay,
                speechRate = state.speechRate,
                onSpeechRateChanged = viewModel::setTtsSpeechRate,
            )

            is ReviewState.Completed -> ReviewCompletionContent(
                knownCount = reviewState.knownCount,
                unknownCount = reviewState.unknownCount,
                missedWords = reviewState.missedWords,
                newStreak = reviewState.newStreak,
                onDismiss = viewModel::acknowledgeCompletion,
            )
        }
    }
}
