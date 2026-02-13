@file:OptIn(ExperimentalTime::class)

package data.word.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.core.database.ProgressRow
import data.core.database.WordEntity
import data.core.database.WordEntityData
import data.word.mapper.WordMapper
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface IWordLocalDataSource {
    suspend fun getAllWordsAsync(): List<Word>
    fun getAllWords(): Flow<List<Word>>
    fun getDueCards(): Flow<List<Word>>
    fun getWordsByStage(stage: LearningStage): Flow<List<Word>>
    suspend fun getWordById(id: Int): Word?
    suspend fun insertWords(words: List<Word>)
    suspend fun updateWord(word: Word)
    suspend fun deleteWord(id: Int)
    suspend fun deleteWords(ids: List<Int>): Int
    suspend fun getAllWordsOnce(): List<WordEntity>
    fun getProgressStats(currentTime: Long): Flow<ProgressStats>
    suspend fun getTotalCount(): Int
    suspend fun getDueCount(): Int
    suspend fun deleteAllWords(): Unit
}

class WordLocalDataSource(
    private val queries: LexiconQueries
) : IWordLocalDataSource {

    override suspend fun getAllWordsAsync(): List<Word> {
        return WordMapper.toDomainList(queries.getAllWords().awaitAsList())
    }

    override fun getAllWords(): Flow<List<Word>> {
        return queries.getAllWords().asFlow().mapToList(Dispatchers.Default)
            .map { WordMapper.toDomainList(it) }
    }

    override fun getDueCards(): Flow<List<Word>> {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return queries.getDueCards(currentTime).asFlow().mapToList(Dispatchers.Default)
            .map { WordMapper.toDomainList(it) }
    }

    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return queries.getWordsByLevel(stage.level.toLong(), currentTime)
            .asFlow().mapToList(Dispatchers.Default)
            .map { WordMapper.toDomainList(it) }
    }

    override suspend fun getWordById(id: Int): Word? {
        return queries.getWordById(id.toLong()).awaitAsOneOrNull()
            ?.let { WordMapper.toDomain(it) }
    }

    override suspend fun insertWords(words: List<Word>) {
        val entities = WordMapper.toEntityDataList(words)
        queries.transaction {
            entities.forEach { entity ->
                queries.upsertWord(
                    id = entity.id.toLong(),
                    originalWord = entity.originalWord,
                    translation = entity.translation,
                    description = entity.description,
                    sourceLanguage = entity.sourceLanguage,
                    targetLanguage = entity.targetLanguage,
                    level = entity.level.toLong(),
                    easeFactor = entity.easeFactor.toDouble(),
                    interval = entity.interval.toLong(),
                    repetitions = entity.repetitions.toLong(),
                    lastReviewDate = entity.lastReviewDate,
                    nextReviewDate = entity.nextReviewDate,
                    dateAdded = entity.dateAdded
                )
            }
        }
    }

    override suspend fun updateWord(word: Word) {
        val entity = WordMapper.toEntityData(word)
        queries.upsertWord(
            id = entity.id.toLong(),
            originalWord = entity.originalWord,
            translation = entity.translation,
            description = entity.description,
            sourceLanguage = entity.sourceLanguage,
            targetLanguage = entity.targetLanguage,
            level = entity.level.toLong(),
            easeFactor = entity.easeFactor.toDouble(),
            interval = entity.interval.toLong(),
            repetitions = entity.repetitions.toLong(),
            lastReviewDate = entity.lastReviewDate,
            nextReviewDate = entity.nextReviewDate,
            dateAdded = entity.dateAdded
        )
    }

    override suspend fun deleteWord(id: Int) {
        queries.deleteWord(id.toLong())
    }

    override suspend fun deleteWords(ids: List<Int>): Int {
        val longIds = ids.map { it.toLong() }
        var deletedCount = 0
        queries.transaction {
            longIds.forEach { id ->
                queries.deleteWord(id)
                deletedCount++
            }
        }
        return deletedCount
    }

    override suspend fun getAllWordsOnce(): List<WordEntity> {
        return queries.getAllWords().awaitAsList()
    }

    override fun getProgressStats(currentTime: Long): Flow<ProgressStats> {
        return queries.progressRow(currentTime).asFlow().mapToOneOrNull(Dispatchers.Default)
            .map { row ->
                if (row != null) {
                    ProgressStats(
                        level0Count = row.level0Count.toInt(),
                        level1Count = row.level1Count.toInt(),
                        level2Count = row.level2Count.toInt(),
                        level3Count = row.level3Count.toInt(),
                        level4Count = row.level4Count.toInt(),
                        level5Count = row.level5Count.toInt(),
                        level6Count = row.level6Count.toInt(),
                        totalWords = row.totalWords.toInt(),
                        dueCards = row.dueCards.toInt()
                    )
                } else {
                    ProgressStats()
                }
            }
    }

    override suspend fun getTotalCount(): Int {
        return queries.countWords().awaitAsOne().toInt()
    }

    override suspend fun getDueCount(): Int {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return queries.countDueCards(currentTime).awaitAsOne().toInt()
    }

    override suspend fun deleteAllWords() {
        queries.deleteAllWords()
    }
}
