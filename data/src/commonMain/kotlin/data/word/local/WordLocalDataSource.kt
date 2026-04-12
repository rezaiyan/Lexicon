package data.word.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.core.database.WordEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

interface IWordLocalDataSource {
    suspend fun getAllWordsAsync(): List<Word>
    fun getAllWords(): Flow<List<Word>>
    fun getDueCards(): Flow<List<Word>>
    fun getDueCardsByTag(tagId: Long): Flow<List<Word>>
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
    suspend fun getNextDueAt(): Long?
    suspend fun deleteAllWords()
    suspend fun getMostCommonSourceLanguage(): String?
}

class WordLocalDataSource(
    private val queries: LexiconQueries,
    private val settingsRepository: ISettingsRepository
) : IWordLocalDataSource {

    override suspend fun getAllWordsAsync(): List<Word> {
        val fallback = settingsRepository.getLanguage().first()
        val entities = queries.getAllWords().awaitAsList()
        if (entities.isEmpty()) return emptyList()
        val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
            .awaitAsList().groupBy { it.wordId }
        return entities.map { entity ->
            entity.toDomain(fallback, tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
        }
    }

    // Combines the word-entity trigger with the word-tag trigger so that tag assignments
    // cause the list to re-emit, keeping tagIds on each Word in sync.
    override fun getAllWords(): Flow<List<Word>> {
        return combine(
            queries.getAllWords().asFlow().mapToList(Dispatchers.Default),
            queries.countWordTags().asFlow().mapToOneOrNull(Dispatchers.Default)
        ) { entities, _ -> entities }
            .map { entities ->
                val language = settingsRepository.getLanguage().first()
                if (entities.isEmpty()) return@map emptyList()
                val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
                    .awaitAsList().groupBy { it.wordId }
                entities.map { entity ->
                    entity.toDomain(language, tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
                }
            }
    }

    override fun getDueCards(): Flow<List<Word>> {
        return combine(
            queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default),
            queries.countWordTags().asFlow().mapToOneOrNull(Dispatchers.Default)
        ) { _, _ -> }
            .map {
                val language = settingsRepository.getLanguage().first()
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val entities = queries.getDueCards(currentTime).awaitAsList()
                if (entities.isEmpty()) return@map emptyList()
                val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
                    .awaitAsList().groupBy { it.wordId }
                entities.map { entity ->
                    entity.toDomain(language, tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
                }
            }
    }

    // Reacts to both word-count changes (review progress) and word-tag changes (tag assignment).
    override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> {
        return combine(
            queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default),
            queries.countWordTags().asFlow().mapToOneOrNull(Dispatchers.Default)
        ) { _, _ -> }
            .map {
                val language = settingsRepository.getLanguage().first()
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val entities = queries.getDueCardsByTag(tagId, currentTime).awaitAsList()
                if (entities.isEmpty()) return@map emptyList()
                val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
                    .awaitAsList().groupBy { it.wordId }
                entities.map { entity ->
                    entity.toDomain(language, tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
                }
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
        val entity = queries.getWordById(id.toLong()).awaitAsOneOrNull() ?: return null
        val tagIds = queries.getTagIdsForWord(id.toLong()).awaitAsList()
        return entity.toDomain(fallback, tagIds)
    }

    override suspend fun insertWords(words: List<Word>) {
        val entities = words.toEntityDataList()
        queries.transaction {
            entities.zip(words).forEach { (entity, word) ->
                if (entity.id == 0) {
                    queries.insertWord(
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
                    // Immediately capture the AUTOINCREMENT ID while still inside the transaction
                    if (word.tagIds.isNotEmpty()) {
                        val realId = queries.lastInsertRowId().executeAsOne()
                        queries.deleteWordTagsForWord(realId)
                        word.tagIds.forEach { tagId -> queries.insertWordTag(realId, tagId) }
                    }
                } else {
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
                    // Only update tags when tagIds is non-empty — an empty list means "no tag info
                    // provided" (e.g. words from a remote sync), so existing local tags are preserved.
                    if (word.tagIds.isNotEmpty()) {
                        queries.deleteWordTagsForWord(word.id.toLong())
                        word.tagIds.forEach { tagId -> queries.insertWordTag(word.id.toLong(), tagId) }
                    }
                }
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

    override suspend fun getNextDueAt(): Long? {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return queries.getNextDueAt(currentTime).awaitAsOne().MIN
    }

    override suspend fun deleteAllWords() {
        queries.deleteAllWords()
    }

    override suspend fun getMostCommonSourceLanguage(): String? {
        return queries.getMostCommonSourceLanguage().awaitAsOneOrNull()
    }
}
