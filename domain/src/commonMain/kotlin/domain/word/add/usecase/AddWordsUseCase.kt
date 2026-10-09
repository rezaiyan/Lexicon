package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.map
import core.error.DomainError
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.AddWordsOutcome
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlin.time.Clock

/**
 * The single write path for new words, whatever their origin.
 * Skips duplicates (inside the batch and against the collection) and reports how many were skipped.
 */
class AddWordsUseCase(
    private val wordRepository: IWordRepository,
    private val clock: Clock,
) : UseCase<AddWordsCommand, AddWordsOutcome> {

    override suspend fun invoke(params: AddWordsCommand): Try<AddWordsOutcome> {
        if (params.drafts.isEmpty()) return Try.failure(DomainError.AddWords.EmptyInput)

        val unique = params.drafts.distinctBy { it.key }
        val duplicatesInBatch = params.drafts.size - unique.size
        val now = clock.now().toEpochMilliseconds()
        val tagIds = params.tagIds.sorted()
        val words = unique.map { draft ->
            Word.newCard(
                term = draft.term,
                translation = draft.translation,
                note = draft.note,
                learningLanguage = params.languages.learning,
                nativeLanguage = params.languages.native,
                tagIds = tagIds,
                nowMillis = now,
            )
        }
        return wordRepository.addWords(words).map { stored ->
            stored.copy(duplicates = stored.duplicates + duplicatesInBatch)
        }
    }
}
