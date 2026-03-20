package domain.onboarding.usecase

import core.common.Try
import core.common.UseCase
import domain.onboarding.model.SuggestedVocabulary
import domain.word.model.Word
import domain.word.repository.IWordRepository
import utils.Language
import kotlin.time.Clock

class ImportSuggestedVocabularyUseCase(
    private val wordRepository: IWordRepository
) : UseCase<List<SuggestedVocabulary>, Int> {
    override suspend operator fun invoke(suggestions: List<SuggestedVocabulary>): Try<Int> {
        val now = Clock.System.now().toEpochMilliseconds()
        val words = suggestions.map { suggestion ->
            Word(
                id = 0,
                originalWord = suggestion.originalWord,
                translation = suggestion.translation,
                description = suggestion.description,
                sourceLanguage = Language.fromCode(Language.toCode(suggestion.sourceLanguage)),
                targetLanguage = Language.fromCode(Language.toCode(suggestion.targetLanguage)),
                level = 0,
                easeFactor = 2.5f,
                interval = 0,
                repetitions = 0,
                lastReviewDate = 0L,
                nextReviewDate = now - 1000,
                dateAdded = now
            )
        }
        return wordRepository.insertWords(words)
    }
}
