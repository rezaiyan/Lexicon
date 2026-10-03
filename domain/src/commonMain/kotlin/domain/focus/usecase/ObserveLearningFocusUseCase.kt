package domain.focus.usecase

import core.common.NoParamFlowUseCase
import domain.focus.LearningFocusPolicy
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Resolved focus: the stored preference validated against the words that actually exist. */
class ObserveLearningFocusUseCase(
    private val wordRepository: IWordRepository,
    private val focusRepository: ILearningFocusRepository,
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : NoParamFlowUseCase<LearningFocus> {

    operator fun invoke(): Flow<LearningFocus> =
        combine(wordRepository.getAllWords(), focusRepository.observePreference()) { words, preference ->
            LearningFocusPolicy.resolve(words, preference, nowMillis())
        }.distinctUntilChanged()

    override operator fun invoke(params: Unit) = invoke()
}
