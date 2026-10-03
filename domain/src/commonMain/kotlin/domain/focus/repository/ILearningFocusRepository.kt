package domain.focus.repository

import core.common.Try
import domain.focus.model.LearningFocus
import kotlinx.coroutines.flow.Flow

/** Local-only preference storage for the learning focus feature. */
interface ILearningFocusRepository {
    /** null = the user never picked a focus. */
    fun observePreference(): Flow<LearningFocus?>
    suspend fun setPreference(focus: LearningFocus): Try<Unit>

    /** Epoch day (UTC) the nudge was last dismissed, or null. */
    fun observeNudgeDismissedDay(): Flow<Long?>
    suspend fun dismissNudge(epochDay: Long): Try<Unit>

    fun observeIntroAcknowledged(): Flow<Boolean>
    suspend fun acknowledgeIntro(): Try<Unit>
}
