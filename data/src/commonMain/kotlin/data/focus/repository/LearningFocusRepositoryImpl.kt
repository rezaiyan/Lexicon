package data.focus.repository

import core.common.Try
import data.focus.local.ILearningFocusLocalDataSource
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import utils.Language

class LearningFocusRepositoryImpl(
    private val localDataSource: ILearningFocusLocalDataSource,
) : ILearningFocusRepository {

    override fun observePreference(): Flow<LearningFocus?> =
        localDataSource.observe().map { it?.focusCode?.toLearningFocus() }.distinctUntilChanged()

    override suspend fun setPreference(focus: LearningFocus): Try<Unit> = Try {
        localDataSource.setFocusCode(focus.toCode())
    }

    override fun observeNudgeDismissedDay(): Flow<Long?> =
        localDataSource.observe().map { it?.nudgeDismissedDay }.distinctUntilChanged()

    override suspend fun dismissNudge(epochDay: Long): Try<Unit> = Try {
        localDataSource.setNudgeDismissedDay(epochDay)
    }

    override fun observeIntroAcknowledged(): Flow<Boolean> =
        localDataSource.observe().map { it?.introAcknowledged ?: false }.distinctUntilChanged()

    override suspend fun acknowledgeIntro(): Try<Unit> = Try {
        localDataSource.setIntroAcknowledged()
    }

    private fun LearningFocus.toCode(): String = when (this) {
        LearningFocus.All -> ALL_CODE
        is LearningFocus.Single -> language.code
    }

    private fun String.toLearningFocus(): LearningFocus =
        if (this == ALL_CODE) LearningFocus.All else LearningFocus.Single(Language.fromCode(this))

    private companion object {
        const val ALL_CODE = "all"
    }
}
