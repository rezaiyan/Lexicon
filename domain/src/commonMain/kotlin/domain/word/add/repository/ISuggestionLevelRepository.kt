package domain.word.add.repository

import core.common.Try
import domain.onboarding.model.ProficiencyLevel

/** Local-only memory of the level the learner last asked AI suggestions for. */
interface ISuggestionLevelRepository {
    /** The level last used, or null when they never generated suggestions. */
    suspend fun lastUsed(): Try<ProficiencyLevel?>
    suspend fun saveLastUsed(level: ProficiencyLevel): Try<Unit>
}
