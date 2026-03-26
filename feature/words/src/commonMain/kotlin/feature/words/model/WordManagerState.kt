package feature.words.model

import domain.tag.model.Tag
import domain.word.model.ImportErrorClassification
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.model.WordSortOption
import utils.Language

data class WordManagerScreenState(
    val words: List<Word> = emptyList(),
    val filteredWords: List<Word> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isUserSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val isDeletingWords: Boolean = false,
    val isBatchUpdatingLanguages: Boolean = false,
    val isBatchAssigningTags: Boolean = false,
    val searchQuery: String = "",
    val sortOption: WordSortOption = WordSortOption.DATE_ADDED_DESC,
    val filterLanguage: Language? = null,
    val filterLearningStage: LearningStage? = null,
    val filterTagId: Long? = null,
    val isSelectionMode: Boolean = false,
    val selectedWordIds: Set<Int> = emptySet(),
    val errorMessage: String? = null,
    val errorClassification: ImportErrorClassification = ImportErrorClassification.GenericError,
) {
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
        get() = searchQuery.isNotBlank() || filterLanguage != null || filterLearningStage != null || filterTagId != null
}

sealed class WordManagerEffect {
    data class WordDeleted(val count: Int) : WordManagerEffect()
    data class WordUpdated(val word: Word) : WordManagerEffect()
    data class WordsLanguageUpdated(val count: Int) : WordManagerEffect()
    data class WordsTagged(val count: Int) : WordManagerEffect()
    data class WordsShared(val count: Int, val text: String, val timestamp: Long) : WordManagerEffect()
    data object ShareFailed : WordManagerEffect()
    data class Error(val message: String) : WordManagerEffect()
}
