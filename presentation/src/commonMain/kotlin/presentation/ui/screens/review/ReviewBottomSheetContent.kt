package presentation.ui.screens.review

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import domain.word.model.Word
import org.koin.compose.viewmodel.koinViewModel
import presentation.model.ReviewType
import presentation.viewmodel.VocabularyViewModel

@Composable
fun ReviewBottomSheetContent(
    title: String,
    reviewType: ReviewType,
    onClose: () -> Unit,
    onReviewComplete: () -> Unit,
    initialWord: Word? = null
) {
    val vocabularyViewModel = koinViewModel<VocabularyViewModel>()
    val currentReviewScreenState by vocabularyViewModel.reviewScreenState.collectAsStateWithLifecycle()

    ReviewBottomSheet(
        modifier = Modifier.safeDrawingPadding(),
        state = currentReviewScreenState.copy(reviewType = reviewType),
        title = title,
        onClose = onClose,
        onReviewComplete = onReviewComplete,
        onReviewWord = vocabularyViewModel::reviewWord,
        onLoadWords = vocabularyViewModel::loadWords,
        onUpdateWord = vocabularyViewModel::updateWord,
        onDeleteWord = vocabularyViewModel::deleteWord,
        initialWord = initialWord
    )
}

