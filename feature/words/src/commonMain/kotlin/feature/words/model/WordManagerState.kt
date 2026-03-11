package feature.words.model

import domain.word.model.LearningStage
import domain.word.model.Word
import utils.Language

data class WordManagerScreenState(
    val words: List<Word> = emptyList(),
    val isUserSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val isDeletingWords: Boolean = false,
    val isBatchUpdatingLanguages: Boolean = false,
    val searchQuery: String = "",
    val sortOption: WordSortOption = WordSortOption.DATE_ADDED_DESC,
    val filterLanguage: Language? = null,
    val filterLearningStage: LearningStage? = null,
    val isSelectionMode: Boolean = false,
    val selectedWordIds: Set<Int> = emptySet(),
    val errorMessage: String? = null
) {
    val filteredWords: List<Word> get() {
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

        // Sort with tiebreakers for stable, predictable ordering
        return when (sortOption) {
            WordSortOption.DATE_ADDED_DESC -> result.sortedWith(
                compareByDescending<Word> { it.dateAdded }.thenByDescending { it.id }
            )
            WordSortOption.DATE_ADDED_ASC -> result.sortedWith(
                compareBy<Word> { it.dateAdded }.thenBy { it.id }
            )
            WordSortOption.ALPHABETICAL_AZ -> result.sortedWith(
                compareBy<Word> { it.originalWord.lowercase() }.thenBy { it.id }
            )
            WordSortOption.ALPHABETICAL_ZA -> result.sortedWith(
                compareByDescending<Word> { it.originalWord.lowercase() }.thenByDescending { it.id }
            )
            WordSortOption.LEVEL_ASC -> result.sortedWith(
                compareBy<Word> { it.level }.thenByDescending { it.dateAdded }.thenByDescending { it.id }
            )
            WordSortOption.LEVEL_DESC -> result.sortedWith(
                compareByDescending<Word> { it.level }.thenByDescending { it.dateAdded }.thenByDescending { it.id }
            )
        }
    }

    val availableLanguages: Set<Language> get() {
        val langs = mutableSetOf<Language>()
        words.forEach { word ->
            langs.add(word.sourceLanguage)
            langs.add(word.targetLanguage)
        }
        return langs
    }

    val selectedCount: Int get() = selectedWordIds.size

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
