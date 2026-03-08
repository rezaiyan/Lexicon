package feature.study.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import domain.tts.model.TtsState
import domain.word.model.Word
import expects.BackHandler
import feature.study.model.ReviewScreenState
import feature.study.model.ReviewType
import core.common.UiState
import overlay.LocalOverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet

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
    initialWord: Word? = null,
    ttsState: TtsState = TtsState.Idle,
    onSpeakClick: (text: String, langCode: String) -> Unit = { _, _ -> }
) {
    val wordListState = state.wordListState
    val reviewType = state.reviewType

    val overlayHost = LocalOverlayHost.current

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var initialIndexApplied by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }
    var reviewedCount by remember { mutableIntStateOf(0) }
    var knownCount by remember { mutableIntStateOf(0) }
    var unknownCount by remember { mutableIntStateOf(0) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var isAutoPlayEnabled by remember { mutableStateOf(false) }

    // Track total words at session start for reviewed count
    val initialWordCount = remember(wordListState) {
        if (wordListState is UiState.Loaded) wordListState.value.size else 0
    }

    // Close button: browse mode dismisses directly;
    // review mode shows exit confirmation bottom sheet.
    val handleClose: () -> Unit = if (reviewType == ReviewType.BROWSE) {
        onClose
    } else {
        { showExitConfirmation = true }
    }

    BackHandler(enabled = reviewType == ReviewType.REVIEW && !showCompletion && !showExitConfirmation) {
        showExitConfirmation = true
    }

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
                words.isEmpty() && !showCompletion -> {
                    if (reviewType == ReviewType.REVIEW && initialWordCount > 0) {
                        reviewedCount = initialWordCount
                        showCompletion = true
                    } else {
                        onReviewComplete()
                    }
                }
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

    // Auto-play pronunciation when enabled and card changes
    LaunchedEffect(currentIndex, isAutoPlayEnabled) {
        if (isAutoPlayEnabled && wordListState is UiState.Loaded) {
            val words = wordListState.value
            val word = words.getOrNull(currentIndex)
            if (word != null) {
                onSpeakClick(word.originalWord, word.targetLanguage.code)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .then(modifier)
    ) {
        if (showCompletion) {
            ReviewCompletionContent(
                knownCount = knownCount,
                unknownCount = unknownCount,
                onDismiss = onReviewComplete
            )
        } else {
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
                        val safeIndex = currentIndex.coerceIn(0, words.size - 1)

                        if (safeIndex < words.size) {
                            ReviewContent(
                                words = words,
                                currentIndex = safeIndex,
                                isFlipped = isFlipped,
                                reviewType = reviewType,
                                title = title,
                                onClose = handleClose,
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
                                },
                                onReview = { rating ->
                                    // Track known/unknown for completion stats
                                    if (rating >= 1) knownCount++ else unknownCount++

                                    handleReview(
                                        words = words,
                                        currentIndex = safeIndex,
                                        rating = rating,
                                        onReviewWord = onReviewWord,
                                        onNext = {
                                            currentIndex++
                                            isFlipped = false
                                        },
                                        onComplete = {
                                            reviewedCount = initialWordCount
                                            showCompletion = true
                                        }
                                    )
                                },
                                onEdit = {
                                    val word = words[safeIndex]
                                    overlayHost.showSizeToFitBottomSheet(tag = "edit-word") { nav ->
                                        EditWordSheetContent(
                                            word = word,
                                            navigator = nav,
                                            onSave = { updatedWord ->
                                                onUpdateWord(updatedWord)
                                            },
                                            onDelete = {
                                                val deletedIndex = currentIndex
                                                val wasLastWord = deletedIndex == words.size - 1
                                                val wasOnlyWord = words.size == 1

                                                onDeleteWord(word.id) {
                                                    if (wasOnlyWord) {
                                                        reviewedCount = initialWordCount
                                                        showCompletion = true
                                                    } else if (wasLastWord) {
                                                        currentIndex = (words.size - 2).coerceAtLeast(0)
                                                        isFlipped = false
                                                    } else {
                                                        isFlipped = false
                                                    }
                                                }
                                            }
                                        )
                                    }
                                },
                                ttsState = ttsState,
                                onSpeakClick = onSpeakClick,
                                isAutoPlayEnabled = isAutoPlayEnabled,
                                onAutoPlayToggle = { isAutoPlayEnabled = it },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        ExitConfirmationBottomSheet(
            onConfirm = onClose,
            onDismiss = { showExitConfirmation = false }
        )
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
