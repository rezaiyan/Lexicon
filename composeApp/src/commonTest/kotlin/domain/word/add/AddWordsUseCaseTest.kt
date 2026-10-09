package domain.word.add

import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrNull
import core.common.getOrThrow
import core.error.DomainError
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.AddWordsOutcome
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.AddWordsUseCase
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class AddWordsUseCaseTest {

    private val now = 1_700_000_000_000L
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(now)
    }
    private val repository = FakeWordRepository()
    private val useCase = AddWordsUseCase(repository, clock)
    private val germanFromEnglish = LanguagePair(learning = Language.GERMAN, native = Language.ENGLISH)

    private fun draft(term: String, translation: String, note: String = "") =
        WordDraft.of(term, translation, note).getOrThrow()

    private fun command(vararg drafts: WordDraft, tagIds: Set<Long> = emptySet()) =
        AddWordsCommand(drafts.toList(), germanFromEnglish, tagIds, WordOrigin.Manual)

    @Test
    fun `invoke stores learning language as targetLanguage and native as sourceLanguage`() = runTest {
        useCase(command(draft("Hund", "dog")))

        val stored = repository.addedWords.single()
        assertEquals("Hund", stored.originalWord)
        assertEquals("dog", stored.translation)
        assertEquals(Language.GERMAN, stored.targetLanguage)
        assertEquals(Language.ENGLISH, stored.sourceLanguage)
    }

    @Test
    fun `invoke creates new cards that are due immediately`() = runTest {
        useCase(command(draft("Hund", "dog", "pet")))

        val stored = repository.addedWords.single()
        assertEquals("pet", stored.description)
        assertEquals(0, stored.level)
        assertEquals(0, stored.repetitions)
        assertEquals(now, stored.dateAdded)
        assertTrue(stored.nextReviewDate <= now)
    }

    @Test
    fun `invoke attaches selected tags`() = runTest {
        useCase(command(draft("Hund", "dog"), tagIds = setOf(3L, 7L)))

        assertEquals(listOf(3L, 7L), repository.addedWords.single().tagIds.sorted())
    }

    @Test
    fun `invoke removes duplicates inside the batch and reports them`() = runTest {
        val outcome = useCase(command(draft("Hund", "dog"), draft("hund", "Dog"), draft("Katze", "cat"))).getOrThrow()

        assertEquals(AddWordsOutcome(2, 1, listOf("Hund", "Katze")), outcome)
        assertEquals(listOf("Hund", "Katze"), repository.addedWords.map { it.originalWord })
    }

    @Test
    fun `invoke counts words already in the collection as duplicates not as failure`() = runTest {
        useCase(command(draft("Hund", "dog")))

        val outcome = useCase(command(draft("Hund", "dog"), draft("Katze", "cat"))).getOrThrow()

        assertEquals(AddWordsOutcome(1, 1, listOf("Katze")), outcome)
    }

    @Test
    fun `invoke when every word exists succeeds with zero added`() = runTest {
        useCase(command(draft("Hund", "dog")))

        val outcome = useCase(command(draft("Hund", "dog"))).getOrThrow()

        assertEquals(AddWordsOutcome(added = 0, duplicates = 1), outcome)
    }

    @Test
    fun `invoke treats the same word in another learning language as new`() = runTest {
        useCase(command(draft("Bank", "bank")))
        val dutch = AddWordsCommand(
            listOf(draft("Bank", "bank")),
            LanguagePair(Language.DUTCH, Language.ENGLISH),
            emptySet(),
            WordOrigin.File,
        )

        val outcome = useCase(dutch).getOrThrow()

        assertEquals(AddWordsOutcome(1, 0, listOf("Bank")), outcome)
    }

    @Test
    fun `invoke with no drafts fails with EmptyInput`() = runTest {
        val error = useCase(command()).exceptionOrNull()

        assertIs<DomainError.AddWords.EmptyInput>(error)
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        repository.addResult = Try.failure(IllegalStateException("db"))

        val result = useCase(command(draft("Hund", "dog")))

        assertEquals("db", result.exceptionOrNull()?.message)
        assertEquals(null, result.getOrNull())
    }
}
