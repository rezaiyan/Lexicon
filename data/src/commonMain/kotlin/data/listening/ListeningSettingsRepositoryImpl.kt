package data.listening

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import core.common.Try
import data.core.database.LexiconQueries
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.listening.repository.IListeningSettingsRepository
import domain.word.model.LearningStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class ListeningSettingsRecord(
    val pauseMs: Long,
    val order: String,
    val repeatCount: Long,
    val sourceKey: String,
    val wordLimit: Long,
    val shuffle: Boolean,
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
            .map { entity ->
                entity?.let {
                    ListeningSettingsRecord(
                        pauseMs = it.pauseMs,
                        order = it.listeningOrder,
                        repeatCount = it.repeatCount,
                        sourceKey = it.sourceKey,
                        wordLimit = it.wordLimit,
                        shuffle = it.shuffle != 0L,
                    )
                }
            }

    override suspend fun save(record: ListeningSettingsRecord) {
        queries.saveListeningSettings(
            pauseMs = record.pauseMs,
            listeningOrder = record.order,
            repeatCount = record.repeatCount,
            sourceKey = record.sourceKey,
            wordLimit = record.wordLimit,
            shuffle = if (record.shuffle) 1L else 0L,
        )
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

private const val SOURCE_DUE = "DUE"
private const val SOURCE_ALL = "ALL"
private const val SOURCE_LEVEL = "LEVEL:"
private const val SOURCE_TAG = "TAG:"

internal fun ListeningSettingsRecord.toDomain() = ListeningSettings(
    pauseMs = pauseMs,
    order = ListeningOrder.entries.firstOrNull { it.name == order } ?: ListeningOrder.WORD_FIRST,
    repeatCount = repeatCount.toInt(),
    selection = ListeningSelection(
        source = sourceKey.toListeningSource(),
        limit = wordLimit.toInt(),
        shuffle = shuffle,
    ),
)

internal fun ListeningSettings.toRecord() = ListeningSettingsRecord(
    pauseMs = pauseMs,
    order = order.name,
    repeatCount = repeatCount.toLong(),
    sourceKey = selection.source.toKey(),
    wordLimit = selection.limit.toLong(),
    shuffle = selection.shuffle,
)

fun ListeningSource.toKey(): String = when (this) {
    ListeningSource.Due -> SOURCE_DUE
    ListeningSource.All -> SOURCE_ALL
    is ListeningSource.Level -> SOURCE_LEVEL + stage.level
    is ListeningSource.Tag -> SOURCE_TAG + tagId
}

/** Unknown or malformed keys fall back to due words, the default source. */
fun String.toListeningSource(): ListeningSource = when {
    this == SOURCE_ALL -> ListeningSource.All
    startsWith(SOURCE_LEVEL) -> removePrefix(SOURCE_LEVEL).toIntOrNull()
        ?.let { level -> LearningStage.entries.firstOrNull { it.level == level } }
        ?.let(ListeningSource::Level)
        ?: ListeningSource.Due
    startsWith(SOURCE_TAG) -> removePrefix(SOURCE_TAG).toLongOrNull()
        ?.let(ListeningSource::Tag)
        ?: ListeningSource.Due
    else -> ListeningSource.Due
}
