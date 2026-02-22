package presentation.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import domain.word.model.Word
import presentation.model.ReviewScreenState
import presentation.model.UiState

@Composable
fun ReviewBottomSheet(
    modifier: Modifier = Modifier,
    state: ReviewScreenState,
    title: String,
    onClose: () -> Unit,
    onReviewComplete: () -> Unit,
    onReviewWord: (Word, Int) -> Unit,
    onLoadWords: () -> Unit,
    onUpdateWord: (Word) -> Unit,
    onDeleteWord: (Int, () -> Unit) -> Unit,
    initialWord: Word? = null
) {
    val wordListState = state.wordListState
    val reviewType = state.reviewType

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Word?>(null) }
    var wordToDelete by remember { mutableStateOf<Word?>(null) }
    var initialIndexApplied by remember { mutableStateOf(false) }

    LaunchedEffect(initialWord, wordListState) {
        if (!initialIndexApplied && initialWord != null && wordListState is UiState.Loaded) {
            val words = wordListState.value
            val index = words.indexOfFirst { it.id == initialWord.id }
            if (index >= 0) {
                currentIndex = index
            }
            initialIndexApplied = true
        }
    }

    // Handle word list changes
    LaunchedEffect(wordListState) {
        if (wordListState is UiState.Loaded) {
            val words = wordListState.value
            when {
                words.isEmpty() -> onReviewComplete()
                currentIndex >= words.size -> {
                    currentIndex = (words.size - 1).coerceAtLeast(0)
                    isFlipped = false
                }
            }
        }
    }

    // Reset flip state when moving to next card
    LaunchedEffect(currentIndex) {
        isFlipped = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .then(modifier)
    ) {
            when (wordListState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(
                    message = wordListState.message,
                    onRetry = onLoadWords
                )

                is UiState.Loaded -> {
                    val words = wordListState.value

                    if (words.isEmpty()) {
                        EmptyState()
                    } else {
                        // Validate index before rendering (only if list is not empty)
                        val safeIndex = currentIndex.coerceIn(0, words.size - 1)

                        if (safeIndex < words.size) {
                            ReviewContent(
                                words = words,
                                currentIndex = safeIndex,
                                isFlipped = isFlipped,
                                reviewType = reviewType,
                                title = title,
                                onClose = onClose,
                                onFlip = { isFlipped = !isFlipped },
                                onNavigateBack = {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        isFlipped = false
                                    }
                                },
                                onNavigateForward = {
                                    if (currentIndex < words.size - 1) {
                                        currentIndex++
                                        isFlipped = false
                                    }
                                    // In browse mode, don't auto-close at end
                                    // User can navigate freely and close manually
                                },
                                onReview = { rating ->
                                    handleReview(
                                        words = words,
                                        currentIndex = safeIndex,
                                        rating = rating,
                                        onReviewWord = onReviewWord,
                                        onNext = {
                                            currentIndex++
                                            isFlipped = false
                                        },
                                        onComplete = onReviewComplete
                                    )
                                },
                                onEdit = {
                                    editingWord = words[safeIndex]
                                }
                            )
                            
                            // Edit Dialog
                            editingWord?.let { word ->
                                EditWordDialog(
                                    word = word,
                                    onDismiss = { editingWord = null },
                                    onSave = { updatedWord ->
                                        onUpdateWord(updatedWord)
                                        editingWord = null
                                    },
                                    onDelete = {
                                        editingWord = null
                                        wordToDelete = word
                                    }
                                )
                            }
                            
                            // Delete Confirmation Dialog
                            wordToDelete?.let { word ->
                                DeleteWordConfirmationDialog(
                                    word = word,
                                    onConfirm = {
                                        val deletedIndex = currentIndex
                                        val wasLastWord = deletedIndex == words.size - 1
                                        val wasOnlyWord = words.size == 1
                                        
                                        onDeleteWord(word.id) {
                                            wordToDelete = null
                                            
                                            // Auto-advance logic
                                            if (wasOnlyWord) {
                                                onReviewComplete()
                                            } else if (wasLastWord) {
                                                // If it was the last word, go back one
                                                currentIndex = (words.size - 2).coerceAtLeast(0)
                                                isFlipped = false
                                            } else {
                                                // If it wasn't the last word, advance
                                                // Note: currentIndex will be adjusted by LaunchedEffect when word list updates
                                                isFlipped = false
                                            }
                                        }
                                    },
                                    onDismiss = { wordToDelete = null }
                                )
                            }
                        }
                    }
                }
            }
        }
}


private fun handleReview(
    words: List<Word>,
    currentIndex: Int,
    rating: Int,
    onReviewWord: (Word, Int) -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    if (currentIndex < words.size) {
        onReviewWord(words[currentIndex], rating)

        val isLastWord = currentIndex == words.size - 1
        if (!isLastWord) {
            onNext()
        } else {
            onComplete()
        }
    }
}
