package domain.word.usecase

import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.model.WordSortOption
import utils.Language

class FilterAndSortWordsUseCase {

    data class Params(
        val words: List<Word>,
        val query: String = "",
        val filterLanguage: Language? = null,
        val filterStage: LearningStage? = null,
        val filterTagId: Long? = null,
        val sortOption: WordSortOption = WordSortOption.DATE_ADDED_DESC,
    )

    operator fun invoke(params: Params): List<Word> {
        var result = params.words

        // Search filter
        if (params.query.isNotBlank()) {
            result = result.filter { word ->
                word.originalWord.contains(params.query, ignoreCase = true) ||
                    word.translation.contains(params.query, ignoreCase = true) ||
                    word.description.contains(params.query, ignoreCase = true)
            }
        }

        // Language filter
        params.filterLanguage?.let { lang ->
            result = result.filter { it.sourceLanguage == lang || it.targetLanguage == lang }
        }

        // Learning stage filter
        params.filterStage?.let { stage ->
            result = result.filter { LearningStage.fromLevel(it.level) == stage }
        }

        // Tag filter
        params.filterTagId?.let { tagId ->
            result = result.filter { it.tagIds.contains(tagId) }
        }

        // Sort with tiebreakers for stable, predictable ordering
        return when (params.sortOption) {
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
}
