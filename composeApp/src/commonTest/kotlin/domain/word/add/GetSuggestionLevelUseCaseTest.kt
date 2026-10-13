package domain.word.add

import core.common.getOrThrow
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.usecase.GetSuggestionLevelUseCase
import fakes.FakeSuggestionLevelRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSuggestionLevelUseCaseTest {

    @Test
    fun `invoke returns the level used last`() = runTest {
        val useCase = GetSuggestionLevelUseCase(FakeSuggestionLevelRepository(ProficiencyLevel.BEGINNER))

        assertEquals(ProficiencyLevel.BEGINNER, useCase(Unit).getOrThrow())
    }

    @Test
    fun `invoke falls back to intermediate when none was used`() = runTest {
        val useCase = GetSuggestionLevelUseCase(FakeSuggestionLevelRepository())

        assertEquals(ProficiencyLevel.INTERMEDIATE, useCase(Unit).getOrThrow())
    }
}
