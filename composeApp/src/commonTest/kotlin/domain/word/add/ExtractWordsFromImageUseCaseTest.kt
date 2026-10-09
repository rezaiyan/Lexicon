package domain.word.add

import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrThrow
import core.error.DomainError
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.usecase.ExtractWordsFromImageUseCase
import fakes.FakeAiRepository
import fakes.FakeImagePreparer
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExtractWordsFromImageUseCaseTest {

    private val preparer = FakeImagePreparer()
    private val ai = FakeAiRepository()
    private val useCase = ExtractWordsFromImageUseCase(preparer, ai)
    private val languages = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val photo = byteArrayOf(1, 2, 3)

    @Test
    fun `invoke sends the prepared image with the language pair`() = runTest {
        preparer.result = Try.success(byteArrayOf(9))
        ai.drafts = Try.success(listOf(WordDraft.of("Hund", "dog").getOrThrow()))

        val drafts = useCase(ExtractWordsFromImageUseCase.Params(photo, languages)).getOrThrow()

        assertEquals(listOf("Hund"), drafts.map { it.term })
        assertContentEquals(byteArrayOf(9), ai.lastImage)
        assertEquals(languages, ai.lastLanguages)
    }

    @Test
    fun `invoke turns the photo as the user rotated it before upload`() = runTest {
        ai.drafts = Try.success(listOf(WordDraft.of("Hund", "dog").getOrThrow()))

        useCase(ExtractWordsFromImageUseCase.Params(photo, languages, quarterTurns = 1)).getOrThrow()

        assertEquals(1, preparer.lastQuarterTurns)
    }

    @Test
    fun `invoke stops before upload when the image cannot be prepared`() = runTest {
        preparer.result = Try.failure(DomainError.AddWords.ImageUnreadable)

        val error = useCase(ExtractWordsFromImageUseCase.Params(photo, languages)).exceptionOrNull()

        assertIs<DomainError.AddWords.ImageUnreadable>(error)
        assertEquals(null, ai.lastImage)
    }

    @Test
    fun `invoke with nothing recognized fails with NothingRecognized`() = runTest {
        ai.drafts = Try.success(emptyList())

        assertIs<DomainError.AddWords.NothingRecognized>(
            useCase(ExtractWordsFromImageUseCase.Params(photo, languages)).exceptionOrNull(),
        )
    }

    @Test
    fun `invoke passes an out-of-credits refusal through unchanged`() = runTest {
        ai.drafts = Try.failure(DomainError.Commerce.InsufficientCredits)

        val error = useCase(ExtractWordsFromImageUseCase.Params(photo, languages)).exceptionOrNull()

        assertIs<DomainError.Commerce.InsufficientCredits>(error)
    }
}
