package domain.word.add

import core.common.getOrThrow
import domain.onboarding.model.SuggestedVocabulary
import domain.word.add.model.AddWordsOutcome
import domain.word.add.model.LanguagePair
import domain.word.add.usecase.AddStarterWordsUseCase
import domain.word.add.usecase.AddWordsUseCase
import fakes.FakeAddWordsLanguageRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

class AddStarterWordsUseCaseTest {

    private val words = FakeWordRepository()
    private val languages = FakeAddWordsLanguageRepository()
    private val useCase = AddStarterWordsUseCase(AddWordsUseCase(words, Clock.System), languages)

    private fun starter(term: String, translation: String) =
        SuggestedVocabulary(term, translation, "", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN)

    @Test
    fun `invoke adds starter words with their own language pair`() = runTest {
        val outcome = useCase(listOf(starter("Hund", "dog"), starter("Katze", "cat"))).getOrThrow()

        assertEquals(AddWordsOutcome(2, 0, listOf("Hund", "Katze")), outcome)
        assertEquals(Language.GERMAN, words.addedWords.first().targetLanguage)
        assertEquals(Language.ENGLISH, words.addedWords.first().sourceLanguage)
    }

    @Test
    fun `invoke remembers the pair as the default for adding words later`() = runTest {
        useCase(listOf(starter("Hund", "dog")))

        assertEquals(LanguagePair(Language.GERMAN, Language.ENGLISH), languages.lastUsed)
    }

    @Test
    fun `invoke with no words adds nothing`() = runTest {
        assertEquals(AddWordsOutcome(0, 0), useCase(emptyList()).getOrThrow())
        assertNull(languages.lastUsed)
    }
}
