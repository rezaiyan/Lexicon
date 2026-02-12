@file:OptIn(ExperimentalTime::class)

package domain.onboarding.usecase

import domain.onboarding.model.SuggestedVocabulary
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ImportSuggestedVocabularyUseCase(
    private val wordRepository: IWordRepository
) {
    suspend operator fun invoke(suggestions: List<SuggestedVocabulary>): Result<Int> {
        val now = Clock.System.now().toEpochMilliseconds()
        val words = suggestions.map { suggestion ->
            Word(
                id = 0,
                originalWord = suggestion.originalWord,
                translation = suggestion.translation,
                description = suggestion.description,
                sourceLanguage = suggestion.sourceLanguage,
                targetLanguage = suggestion.targetLanguage,
                level = 0,
                easeFactor = 2.5f,
                interval = 0,
                repetitions = 0,
                lastReviewDate = 0L,
                nextReviewDate = now - 1000,
                dateAdded = now
            )
        }
        return Result.success(wordRepository.insertWords(words))
    }
}
