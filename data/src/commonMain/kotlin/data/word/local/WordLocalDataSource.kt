@file:OptIn(ExperimentalTime::class)

package data.word.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.core.database.WordEntity
import data.core.database.WordEntityData
import data.word.mapper.toDomain
import data.word.mapper.toDomainList
import data.word.mapper.toEntityData
import data.word.mapper.toEntityDataList
import domain.settings.repository.ISettingsRepository
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    suspend fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Int
    suspend fun getAllWordsOnce(): List<WordEntity>
    fun getProgressStats(): Flow<ProgressStats>
    suspend fun getTotalCount(): Int
    suspend fun getDueCount(): Int
    suspend fun deleteAllWords(): Unit
}

class WordLocalDataSource(
    private val queries: LexiconQueries,
    private val settingsRepository: ISettingsRepository
) : IWordLocalDataSource {

    override suspend fun getAllWordsAsync(): List<Word> {
        val fallback = settingsRepository.getLanguage().first()
        return queries.getAllWords().awaitAsList().toDomainList(fallback)
    }

    override fun getAllWords(): Flow<List<Word>> {
        return queries.getAllWords().asFlow().mapToList(Dispatchers.Default)
            .map { entities ->
                val language = settingsRepository.getLanguage().first()
                entities.toDomainList(language)
            }
    }

    override fun getDueCards(): Flow<List<Word>> {
        return queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map {
                val language = settingsRepository.getLanguage().first()
                val currentTime = Clock.System.now().toEpochMilliseconds()
                queries.getDueCards(currentTime).awaitAsList().toDomainList(language)
            }
    }

    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> {
        return queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map {
                val language = settingsRepository.getLanguage().first()
                val currentTime = Clock.System.now().toEpochMilliseconds()
                queries.getWordsByLevel(stage.level.toLong(), currentTime)
                    .awaitAsList().toDomainList(language)
            }
    }

    override suspend fun getWordById(id: Int): Word? {
        val fallback = settingsRepository.getLanguage().first()
        return queries.getWordById(id.toLong()).awaitAsOneOrNull()
            ?.toDomain(fallback)
    }

    override suspend fun insertWords(words: List<Word>) {
        val entities = words.toEntityDataList()
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
        val entity = word.toEntityData()
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
        if (ids.isEmpty()) return 0
        val longIds = ids.map { it.toLong() }
        queries.deleteWords(longIds)
        return ids.size
    }

    override suspend fun updateWordsLanguages(
        ids: List<Int>,
        sourceLanguage: String,
        targetLanguage: String
    ): Int {
        if (ids.isEmpty()) return 0
        val longIds = ids.map { it.toLong() }
        queries.updateWordLanguages(sourceLanguage, targetLanguage, longIds)
        return ids.size
    }

    override suspend fun getAllWordsOnce(): List<WordEntity> {
        return queries.getAllWords().awaitAsList()
    }

    override fun getProgressStats(): Flow<ProgressStats> {
        return queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map {
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val row = queries.progressRow(currentTime).awaitAsOneOrNull()
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
