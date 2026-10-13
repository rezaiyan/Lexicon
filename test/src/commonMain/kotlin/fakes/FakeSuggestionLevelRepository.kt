package fakes

import core.common.Try
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.repository.ISuggestionLevelRepository

class FakeSuggestionLevelRepository(
    var lastUsed: ProficiencyLevel? = null,
) : ISuggestionLevelRepository {
    override suspend fun lastUsed(): Try<ProficiencyLevel?> = Try.success(lastUsed)

    override suspend fun saveLastUsed(level: ProficiencyLevel): Try<Unit> {
        lastUsed = level
        return Try.success(Unit)
    }
}
