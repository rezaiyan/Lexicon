package feature.study.ui.review

import androidx.compose.runtime.Composable
import domain.tts.model.TtsState
import domain.word.model.Word
import feature.study.model.ReviewScreenState
import feature.study.model.ReviewType

/**
 * Pure UI component for the review screen.
 * Hosted inside a [overlay.fullscreen.FullScreenOverlay] (not a ModalBottomSheet)
 * so that child bottom sheets (e.g. EditWordBottomSheet) can open on top without nesting issues.
 */
@Composable
fun ReviewBottomSheetContent(
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
        state = reviewState.copy(reviewType = reviewType),
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

