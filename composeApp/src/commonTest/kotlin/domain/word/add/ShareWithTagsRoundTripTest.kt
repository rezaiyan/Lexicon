package domain.word.add

import core.common.getOrThrow
import domain.tag.model.Tag
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.AddWordsOutcome
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordFile
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.AddWordsUseCase
import domain.word.add.usecase.ParseWordFileUseCase
import domain.word.model.Word
import domain.word.usecase.ExportWordsUseCase
import fakes.FakeTagRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Words shared from the word list with "Include tags" and imported as a file on another account:
 * export -> file bytes -> parse -> add. Checks which tags end up on which word and which tags get created.
 */
class ShareWithTagsRoundTripTest {

    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    }
    private val receiverWords = FakeWordRepository()
    private val receiverTags = FakeTagRepository()
    private val addWords = AddWordsUseCase(receiverWords, receiverTags, clock)
    private val languages = LanguagePair(learning = Language.GERMAN, native = Language.ENGLISH)

    private val senderTags = listOf(
        Tag(1L, "Animals", createdAt = 0L, updatedAt = 0L),
        Tag(2L, "A1", createdAt = 0L, updatedAt = 0L),
        Tag(3L, "verbs, irregular | strong", createdAt = 0L, updatedAt = 0L),
    )

    private fun word(id: Int, term: String, translation: String, note: String = "", tagIds: List<Long> = emptyList()) =
        Word(
            id = id,
            originalWord = term,
            translation = translation,
            description = note,
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.GERMAN,
            level = 0,
            easeFactor = 2.5f,
            interval = 0,
            repetitions = 0,
            lastReviewDate = 0L,
            nextReviewDate = 0L,
            tagIds = tagIds,
        )

    private val shared = listOf(
        word(1, "Hund", "dog", "a pet, loyal", tagIds = listOf(1L, 2L)),
        word(2, "Katze", "cat", tagIds = listOf(1L)),
        word(3, "gehen", "to go; to walk", "ging\ngegangen", tagIds = listOf(3L, 2L)),
        word(4, "Haus", "house"),
    )

    /** What the word list's Share does with "Include tags" checked, then what the file import does. */
    private suspend fun shareThenImport(words: List<Word>, selectedTagIds: Set<Long> = emptySet()): AddWordsOutcome {
        val text = ExportWordsUseCase()(ExportWordsUseCase.Params(words, senderTags.associate { it.id to it.name }))
            .getOrThrow()
        val report = ParseWordFileUseCase()(WordFile("vokab_words_4_0.txt", text.encodeToByteArray())).getOrThrow()
        assertEquals(emptyList(), report.rejected)
        return addWords(AddWordsCommand(report.drafts, languages, selectedTagIds, WordOrigin.File)).getOrThrow()
    }

    private fun tagNamesOf(term: String): List<String> {
        val byId = receiverTags.tags.associate { it.id to it.name }
        return receiverWords.addedWords.single { it.originalWord == term }.tagIds.map { byId.getValue(it) }
    }

    @Test
    fun `import on a fresh account creates each shared tag once and attaches it to the right words`() = runTest {
        val outcome = shareThenImport(shared)

        assertEquals(4, outcome.added)
        assertEquals(listOf("Animals", "A1", "verbs irregular strong"), receiverTags.createdNames)
        assertEquals(listOf("Animals", "A1"), tagNamesOf("Hund"))
        assertEquals(listOf("Animals"), tagNamesOf("Katze"))
        assertEquals(setOf("A1", "verbs irregular strong"), tagNamesOf("gehen").toSet())
        assertEquals(emptyList(), tagNamesOf("Haus"))
    }

    @Test
    fun `import keeps notes and translations that hold separators and line breaks`() = runTest {
        shareThenImport(shared)

        val byTerm = receiverWords.addedWords.associateBy { it.originalWord }
        assertEquals("a pet, loyal", byTerm.getValue("Hund").description)
        assertEquals("to go; to walk", byTerm.getValue("gehen").translation)
        assertEquals("ging\ngegangen", byTerm.getValue("gehen").description)
    }

    @Test
    fun `import reuses the receiver's tags matching names case-insensitively`() = runTest {
        receiverTags.tags = listOf(Tag(50L, "animals", createdAt = 0L, updatedAt = 0L), Tag(51L, " a1 ", createdAt = 0L, updatedAt = 0L))

        shareThenImport(shared)

        assertEquals(listOf("verbs irregular strong"), receiverTags.createdNames)
        assertEquals(listOf(50L, 51L), receiverWords.addedWords.single { it.originalWord == "Hund" }.tagIds)
    }

    @Test
    fun `import adds the tags picked on the import screen next to the shared ones`() = runTest {
        receiverTags.tags = listOf(Tag(7L, "From Ali", createdAt = 0L, updatedAt = 0L))

        shareThenImport(shared, selectedTagIds = setOf(7L))

        assertEquals(listOf("From Ali", "Animals"), tagNamesOf("Katze"))
        assertEquals(listOf("From Ali"), tagNamesOf("Haus"))
    }

    @Test
    fun `a tag the sender's word points at but that no longer exists is left out`() = runTest {
        shareThenImport(listOf(word(9, "Baum", "tree", tagIds = listOf(1L, 404L))))

        assertEquals(listOf("Animals"), receiverTags.createdNames)
        assertEquals(listOf("Animals"), tagNamesOf("Baum"))
    }

    @Test
    fun `a word the receiver already has is skipped and does not get the shared tags`() = runTest {
        receiverWords.storedWords += word(77, "Hund", "dog")

        val outcome = shareThenImport(shared)

        assertEquals(3, outcome.added)
        assertEquals(1, outcome.duplicates)
        assertEquals(emptyList(), receiverWords.storedWords.first { it.id == 77 }.tagIds)
    }
}
