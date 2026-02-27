package presentation.model

import domain.word.model.LearningStage
import domain.word.model.Word
import utils.Language

data class WordManagerScreenState(
    val words: List<Word> = emptyList(),
    val isUserSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val isDeletingWords: Boolean = false,
    val isBatchUpdatingLanguages: Boolean = false,
    val showBatchEditLanguages: Boolean = false,
    val searchQuery: String = "",
    val sortOption: WordSortOption = WordSortOption.DATE_ADDED_DESC,
    val filterLanguage: Language? = null,
    val filterLearningStage: LearningStage? = null,
    val isSelectionMode: Boolean = false,
    val selectedWordIds: Set<Int> = emptySet(),
    val detailWord: Word? = null,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredWords: List<Word> by lazy {
        var result = words

        // Search filter
        if (searchQuery.isNotBlank()) {
            result = result.filter { word ->
                word.originalWord.contains(searchQuery, ignoreCase = true) ||
                    word.translation.contains(searchQuery, ignoreCase = true) ||
                    word.description.contains(searchQuery, ignoreCase = true)
            }
        }

        // Language filter
        filterLanguage?.let { lang ->
            result = result.filter { it.sourceLanguage == lang || it.targetLanguage == lang }
        }

        // Learning stage filter
        filterLearningStage?.let { stage ->
            result = result.filter { LearningStage.fromLevel(it.level) == stage }
        }

        // Sort
        when (sortOption) {
            WordSortOption.DATE_ADDED_DESC -> result.sortedByDescending { it.dateAdded }
            WordSortOption.DATE_ADDED_ASC -> result.sortedBy { it.dateAdded }
            WordSortOption.ALPHABETICAL_AZ -> result.sortedBy { it.originalWord.lowercase() }
            WordSortOption.ALPHABETICAL_ZA -> result.sortedByDescending { it.originalWord.lowercase() }
            WordSortOption.LEVEL_ASC -> result.sortedBy { it.level }
            WordSortOption.LEVEL_DESC -> result.sortedByDescending { it.level }
        }
    }

    val availableLanguages: Set<Language> by lazy {
        val langs = mutableSetOf<Language>()
        words.forEach { word ->
            langs.add(word.sourceLanguage)
            langs.add(word.targetLanguage)
        }
        langs
    }

    fun isWordSelected(wordId: Int): Boolean = selectedWordIds.contains(wordId)

    val selectedCount: Int get() = selectedWordIds.size

    val areAllSelected: Boolean
        get() =
            filteredWords.isNotEmpty() &&
                filteredWords.all { selectedWordIds.contains(it.id) }

    val isFiltered: Boolean
        get() = searchQuery.isNotBlank() || filterLanguage != null || filterLearningStage != null
}

sealed class WordManagerEffect {
    data class WordDeleted(val count: Int) : WordManagerEffect()
    data class WordUpdated(val word: Word) : WordManagerEffect()
    data class WordsLanguageUpdated(val count: Int) : WordManagerEffect()
    data class WordsShared(val count: Int, val text: String, val timestamp: Long) : WordManagerEffect()
    data object ShareFailed : WordManagerEffect()
    data class Error(val message: String) : WordManagerEffect()
}
