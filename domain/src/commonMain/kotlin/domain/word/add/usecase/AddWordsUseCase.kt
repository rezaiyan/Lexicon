package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrNull
import core.common.map
import core.error.DomainError
import domain.tag.repository.ITagRepository
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.AddWordsOutcome
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * The single write path for new words, whatever their origin.
 * Skips duplicates (inside the batch and against the collection) and reports how many were skipped.
 * Tags named by a draft are matched to existing tags by name (ignoring case) or created; a tag that
 * can't be created (e.g. offline) is skipped so the words are still added.
 */
class AddWordsUseCase(
    private val wordRepository: IWordRepository,
    private val tagRepository: ITagRepository,
    private val clock: Clock,
) : UseCase<AddWordsCommand, AddWordsOutcome> {

    override suspend fun invoke(params: AddWordsCommand): Try<AddWordsOutcome> {
        if (params.drafts.isEmpty()) return Try.failure(DomainError.AddWords.EmptyInput)

        val unique = params.drafts.distinctBy { it.key }
        val duplicatesInBatch = params.drafts.size - unique.size
        val now = clock.now().toEpochMilliseconds()
        val namedTagIds = resolveTagIds(unique.flatMap { it.tags })
        val words = unique.map { draft ->
            val tagIds = (params.tagIds + draft.tags.mapNotNull { namedTagIds[it.lowercase()] }).sorted()
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

    /** Tag ids keyed by lowercase name. */
    private suspend fun resolveTagIds(names: List<String>): Map<String, Long> {
        if (names.isEmpty()) return emptyMap()
        val ids = tagRepository.getTags().first().associateTo(mutableMapOf()) { it.name.trim().lowercase() to it.id }
        val missing = names.distinctBy { it.lowercase() }.filterNot { it.lowercase() in ids }
        for (name in missing) {
            // Creating a tag needs the server; after one failure (likely offline) don't wait on the rest.
            val tag = tagRepository.createTag(name).getOrNull() ?: break
            ids[name.lowercase()] = tag.id
        }
        return ids
    }
}
