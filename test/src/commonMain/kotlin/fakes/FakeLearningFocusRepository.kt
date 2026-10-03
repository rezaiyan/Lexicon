package fakes

import core.common.Try
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLearningFocusRepository(
    initialPreference: LearningFocus? = null,
) : ILearningFocusRepository {
    val preference = MutableStateFlow(initialPreference)
    val nudgeDismissedDay = MutableStateFlow<Long?>(null)
    val introAcknowledged = MutableStateFlow(false)

    override fun observePreference(): Flow<LearningFocus?> = preference
    override suspend fun setPreference(focus: LearningFocus): Try<Unit> {
        preference.value = focus
        return Try.success(Unit)
    }

    override fun observeNudgeDismissedDay(): Flow<Long?> = nudgeDismissedDay
    override suspend fun dismissNudge(epochDay: Long): Try<Unit> {
        nudgeDismissedDay.value = epochDay
        return Try.success(Unit)
    }

    override fun observeIntroAcknowledged(): Flow<Boolean> = introAcknowledged
    override suspend fun acknowledgeIntro(): Try<Unit> {
        introAcknowledged.value = true
        return Try.success(Unit)
    }
}
