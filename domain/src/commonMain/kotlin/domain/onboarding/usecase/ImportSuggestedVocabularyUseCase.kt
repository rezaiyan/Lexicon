package domain.onboarding.usecase

import core.common.Try
import core.common.UseCase
import domain.onboarding.model.SuggestedVocabulary
import domain.word.model.Word
import domain.word.repository.IWordRepository
import utils.Language
import kotlin.time.Clock

class ImportSuggestedVocabularyUseCase(
    private val wordRepository: IWordRepository,
) : UseCase<ImportSuggestedVocabularyUseCase.Params, Int> {

    data class Params(
        val suggestions: List<SuggestedVocabulary>,
        val tagId: Long? = null,
    )

    override suspend operator fun invoke(params: Params): Try<Int> {
        val now = Clock.System.now().toEpochMilliseconds()
        val tagIds = if (params.tagId != null) listOf(params.tagId) else emptyList()
        val words = params.suggestions.map { suggestion ->
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
                dateAdded = now,
                tagIds = tagIds,
            )
        }
        return wordRepository.insertWords(words)
    }
}
