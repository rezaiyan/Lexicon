package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrNull
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.repository.ISuggestionLevelRepository

/**
 * The level to preselect for AI suggestions: the one the learner used last, so they set it once,
 * else [DEFAULT]. Never fails; an unreadable memory falls back to the default.
 */
class GetSuggestionLevelUseCase(
    private val levelRepository: ISuggestionLevelRepository,
) : UseCase<Unit, ProficiencyLevel> {

    override suspend fun invoke(params: Unit): Try<ProficiencyLevel> =
        Try.success(levelRepository.lastUsed().getOrNull() ?: DEFAULT)

    companion object {
        /** The middle of the range: easy to step up or down from in one tap. */
        val DEFAULT = ProficiencyLevel.INTERMEDIATE
    }
}
