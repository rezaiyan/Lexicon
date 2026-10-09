package domain.word.add

import core.common.getOrThrow
import domain.focus.model.LearningFocus
import domain.word.add.model.LanguagePair
import domain.word.add.usecase.ResolveAddWordsLanguagesUseCase
import fakes.FakeAddWordsLanguageRepository
import fakes.FakeLearningFocusRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveAddWordsLanguagesUseCaseTest {

    private val languages = FakeAddWordsLanguageRepository()
    private val focus = FakeLearningFocusRepository()
    private val useCase = ResolveAddWordsLanguagesUseCase(languages, focus)

    private val germanEnglish = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val dutchEnglish = LanguagePair(Language.DUTCH, Language.ENGLISH)

    @Test
    fun `invoke with nothing known returns null so the user is asked`() = runTest {
        assertNull(useCase(Unit).getOrThrow())
    }

    @Test
    fun `invoke prefers the last used pair`() = runTest {
        languages.lastUsed = dutchEnglish
        languages.mostCommon = germanEnglish

        assertEquals(dutchEnglish, useCase(Unit).getOrThrow())
    }

    @Test
    fun `invoke falls back to the pair most words use`() = runTest {
        languages.mostCommon = germanEnglish

        assertEquals(germanEnglish, useCase(Unit).getOrThrow())
    }

    @Test
    fun `invoke follows the learning focus when it differs from the last used pair`() = runTest {
        languages.lastUsed = dutchEnglish
        focus.setPreference(LearningFocus.Single(Language.GERMAN))

        assertEquals(germanEnglish, useCase(Unit).getOrThrow())
    }

    @Test
    fun `invoke keeps the last used pair when it matches the focus`() = runTest {
        languages.lastUsed = LanguagePair(Language.GERMAN, Language.PERSIAN)
        focus.setPreference(LearningFocus.Single(Language.GERMAN))

        assertEquals(LanguagePair(Language.GERMAN, Language.PERSIAN), useCase(Unit).getOrThrow())
    }

    @Test
    fun `invoke ignores focus All`() = runTest {
        languages.lastUsed = dutchEnglish
        focus.setPreference(LearningFocus.All)

        assertEquals(dutchEnglish, useCase(Unit).getOrThrow())
    }
}
