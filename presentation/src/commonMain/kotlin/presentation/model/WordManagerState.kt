package presentation.model

import domain.word.model.Word

/**
 * State for Word Manager screen
 */
data class WordManagerScreenState(
    val words: List<Word> = emptyList(),
    val isUserSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val isDeletingWords: Boolean = false,
    val searchQuery: String = "",
    val isMultiSelectMode: Boolean = false,
    val selectedWordIds: Set<Int> = emptySet(),
    val editingWord: Word? = null,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
) {
    /**
     * Filtered words based on search query
     */
    val filteredWords: List<Word> = if (searchQuery.isBlank()) {
        words
    } else {
        words.filter { word ->
            word.originalWord.contains(searchQuery, ignoreCase = true) ||
            word.translation.contains(searchQuery, ignoreCase = true) ||
            word.description.contains(searchQuery, ignoreCase = true)
        }
    }
    
    /**
     * Check if a word is selected
     */
    fun isWordSelected(wordId: Int): Boolean = selectedWordIds.contains(wordId)
    
    /**
     * Get count of selected words
     */
    val selectedCount: Int get() = selectedWordIds.size
    
    /**
     * Check if all visible words are selected
     */
    val areAllSelected: Boolean get() = 
        filteredWords.isNotEmpty() && 
        filteredWords.all { selectedWordIds.contains(it.id) }
}

/**
 * Events for Word Manager
 */
sealed class WordManagerEvent {
    data class WordDeleted(val count: Int) : WordManagerEvent()
    data class WordUpdated(val word: Word) : WordManagerEvent()
    data class WordsShared(val count: Int, val text: String, val timestamp: Long) : WordManagerEvent()
    data object ShareFailed : WordManagerEvent()
    data class Error(val message: String) : WordManagerEvent()
}

