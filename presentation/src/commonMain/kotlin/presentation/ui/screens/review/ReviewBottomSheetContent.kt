package presentation.ui.screens.review

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import domain.tts.model.TtsState
import domain.word.model.Word
import presentation.model.ReviewScreenState
import presentation.model.ReviewType

/**
 * Pure UI component for review bottom sheet.
 * Receives state and callbacks from parent ViewModel - does NOT inject its own ViewModel.
 */
@Composable
fun ReviewBottomSheetContent(
    title: String,
    reviewType: ReviewType,
    reviewState: ReviewScreenState,
    onClose: () -> Unit,
    onReviewComplete: () -> Unit,
    onReviewWord: (Word, Int) -> Unit,
    onLoadWords: () -> Unit,
    onUpdateWord: (Word) -> Unit,
    onDeleteWord: (Int, () -> Unit) -> Unit,
    initialWord: Word? = null,
    ttsState: TtsState = TtsState.Idle,
    onSpeakClick: (text: String, langCode: String) -> Unit = { _, _ -> }
) {
    ReviewBottomSheet(
        modifier = Modifier.safeDrawingPadding(),
        state = reviewState.copy(reviewType = reviewType),
        title = title,
        onClose = onClose,
        onReviewComplete = onReviewComplete,
        onReviewWord = onReviewWord,
        onLoadWords = onLoadWords,
        onUpdateWord = onUpdateWord,
        onDeleteWord = onDeleteWord,
        initialWord = initialWord,
        ttsState = ttsState,
        onSpeakClick = onSpeakClick
    )
}

