package data.listening

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import core.common.Try
import data.core.database.LexiconQueries
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSettings
import domain.listening.repository.IListeningSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class ListeningSettingsRecord(
    val pauseMs: Long,
    val order: String,
    val repeatCount: Long,
)

interface IListeningSettingsLocalDataSource {
    fun observe(): Flow<ListeningSettingsRecord?>
    suspend fun save(record: ListeningSettingsRecord)
}

class ListeningSettingsLocalDataSourceImpl(
    private val queries: LexiconQueries,
) : IListeningSettingsLocalDataSource {

    override fun observe(): Flow<ListeningSettingsRecord?> =
        queries.getListeningSettings().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map { entity -> entity?.let { ListeningSettingsRecord(it.pauseMs, it.listeningOrder, it.repeatCount) } }

    override suspend fun save(record: ListeningSettingsRecord) {
        queries.saveListeningSettings(record.pauseMs, record.order, record.repeatCount)
    }
}

class ListeningSettingsRepositoryImpl(
    private val localDataSource: IListeningSettingsLocalDataSource,
) : IListeningSettingsRepository {

    override fun observe(): Flow<ListeningSettings> =
        localDataSource.observe().map { it?.toDomain() ?: ListeningSettings() }.distinctUntilChanged()

    override suspend fun save(settings: ListeningSettings): Try<Unit> = Try {
        localDataSource.save(settings.toRecord())
    }
}

internal fun ListeningSettingsRecord.toDomain() = ListeningSettings(
    pauseMs = pauseMs,
    order = ListeningOrder.entries.firstOrNull { it.name == order } ?: ListeningOrder.WORD_FIRST,
    repeatCount = repeatCount.toInt(),
)

internal fun ListeningSettings.toRecord() = ListeningSettingsRecord(
    pauseMs = pauseMs,
    order = order.name,
    repeatCount = repeatCount.toLong(),
)
