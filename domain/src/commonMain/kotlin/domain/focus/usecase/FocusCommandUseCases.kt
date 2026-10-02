package domain.focus.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.UseCase
import domain.focus.LearningFocusPolicy
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository

class SetLearningFocusUseCase(
    private val focusRepository: ILearningFocusRepository,
) : UseCase<LearningFocus, Unit> {
    override suspend operator fun invoke(params: LearningFocus): Try<Unit> =
        focusRepository.setPreference(params)
}

class DismissFocusNudgeUseCase(
    private val focusRepository: ILearningFocusRepository,
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : NoParamUseCase<Unit> {
    suspend operator fun invoke(): Try<Unit> =
        focusRepository.dismissNudge(LearningFocusPolicy.epochDay(nowMillis()))

    override suspend operator fun invoke(params: Unit) = invoke()
}

class AcknowledgeFocusIntroUseCase(
    private val focusRepository: ILearningFocusRepository,
) : NoParamUseCase<Unit> {
    suspend operator fun invoke(): Try<Unit> = focusRepository.acknowledgeIntro()

    override suspend operator fun invoke(params: Unit) = invoke()
}
