package data.focus.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LearningFocusRecord(
    val focusCode: String?,
    val nudgeDismissedDay: Long?,
    val introAcknowledged: Boolean,
)

interface ILearningFocusLocalDataSource {
    fun observe(): Flow<LearningFocusRecord?>
    suspend fun setFocusCode(code: String)
    suspend fun setNudgeDismissedDay(day: Long)
    suspend fun setIntroAcknowledged()
}

class LearningFocusLocalDataSourceImpl(
    private val queries: LexiconQueries,
) : ILearningFocusLocalDataSource {

    override fun observe(): Flow<LearningFocusRecord?> =
        queries.getLearningFocus().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map { entity ->
                entity?.let {
                    LearningFocusRecord(
                        focusCode = it.focusCode,
                        nudgeDismissedDay = it.nudgeDismissedDay,
                        introAcknowledged = it.introAcknowledged != 0L,
                    )
                }
            }

    override suspend fun setFocusCode(code: String) {
        queries.ensureLearningFocusRow()
        queries.setLearningFocusCode(code)
    }

    override suspend fun setNudgeDismissedDay(day: Long) {
        queries.ensureLearningFocusRow()
        queries.setLearningFocusNudgeDismissedDay(day)
    }

    override suspend fun setIntroAcknowledged() {
        queries.ensureLearningFocusRow()
        queries.setLearningFocusIntroAcknowledged()
    }
}
