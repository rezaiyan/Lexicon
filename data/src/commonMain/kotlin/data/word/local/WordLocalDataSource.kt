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
import domain.word.add.model.AddWordsOutcome
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.model.WordIdentity
import utils.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    /** Inserts words whose identity is new, in one transaction, and queues them for upload. */
    suspend fun addNewWords(words: List<Word>): AddWordsOutcome
    suspend fun getPendingUploads(): List<Word>
    /**
     * Marks [uploaded] as sent and moves each word in [serverIds] (local id → server id) to its server id,
     * with its tags and pending review, so later edits and deletes address the right server word.
     */
    suspend fun completeUpload(uploaded: List<Int>, serverIds: Map<Int, Int>)
}

class WordLocalDataSource(
    private val queries: LexiconQueries,
) : IWordLocalDataSource {

    override suspend fun getAllWordsAsync(): List<Word> {
        val entities = queries.getAllWords().awaitAsList()
        if (entities.isEmpty()) return emptyList()
        val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
            .awaitAsList().groupBy { it.wordId }
        return entities.map { entity ->
            entity.toDomain(tagIds = tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
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
                if (entities.isEmpty()) return@map emptyList()
                val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
                    .awaitAsList().groupBy { it.wordId }
                entities.map { entity ->
                    entity.toDomain(tagIds = tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
                }
            }
    }

    override fun getDueCards(): Flow<List<Word>> {
        return combine(
            queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default),
            queries.countWordTags().asFlow().mapToOneOrNull(Dispatchers.Default)
        ) { _, _ -> }
            .map {
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val entities = queries.getDueCards(currentTime).awaitAsList()
                if (entities.isEmpty()) return@map emptyList()
                val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
                    .awaitAsList().groupBy { it.wordId }
                entities.map { entity ->
                    entity.toDomain(tagIds = tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
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
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val entities = queries.getDueCardsByTag(tagId, currentTime).awaitAsList()
                if (entities.isEmpty()) return@map emptyList()
                val tagMappings = queries.getTagMappingsForWords(entities.map { it.id })
                    .awaitAsList().groupBy { it.wordId }
                entities.map { entity ->
                    entity.toDomain(tagIds = tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
                }
            }
    }

    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> {
        return queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map {
                val currentTime = Clock.System.now().toEpochMilliseconds()
                queries.getWordsByLevel(stage.level.toLong(), currentTime)
                    .awaitAsList().toDomainList()
            }
    }

    override suspend fun getWordById(id: Int): Word? {
        val entity = queries.getWordById(id.toLong()).awaitAsOneOrNull() ?: return null
        val tagIds = queries.getTagIdsForWord(id.toLong()).awaitAsList()
        return entity.toDomain(tagIds = tagIds)
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

    // Tag links have no FK cascade, so every word delete removes the word's WordTagEntity rows too;
    // otherwise tag word counts keep counting deleted words.
    override suspend fun deleteWord(id: Int) {
        queries.transaction {
            queries.deleteWordTagsForWord(id.toLong())
            queries.deleteWord(id.toLong())
            queries.removeWordUploads(listOf(id.toLong()))
        }
    }

    override suspend fun deleteWords(ids: List<Int>): Int {
        if (ids.isEmpty()) return 0
        val longIds = ids.map { it.toLong() }
        queries.transaction {
            queries.deleteWordTagsForWords(longIds)
            queries.deleteWords(longIds)
            queries.removeWordUploads(longIds)
        }
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
        queries.transaction {
            queries.deleteAllWordTags()
            queries.deleteAllWords()
            queries.clearWordUploadQueue()
        }
    }

    override suspend fun getMostCommonSourceLanguage(): String? {
        return queries.getMostCommonSourceLanguage().awaitAsOneOrNull()
    }

    override suspend fun addNewWords(words: List<Word>): AddWordsOutcome {
        val addedTerms = mutableListOf<String>()
        queries.transaction {
            val known = words.map { it.targetLanguage }.distinct().flatMapTo(mutableSetOf()) { language ->
                queries.getWordIdentitiesForLanguage(language.code).awaitAsList().map { row ->
                    WordIdentity(row.originalWord.trim().lowercase(), row.translation.trim().lowercase(), language)
                }
            }
            words.filter { known.add(it.identity) }.forEach { word ->
                val entity = word.toEntityData()
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
                    dateAdded = entity.dateAdded,
                )
                val id = queries.lastInsertRowId().awaitAsOne()
                word.tagIds.forEach { tagId -> queries.insertWordTag(id, tagId) }
                queries.enqueueWordUpload(id)
                addedTerms += word.originalWord
            }
        }
        return AddWordsOutcome(addedTerms.size, words.size - addedTerms.size, addedTerms)
    }

    override suspend fun getPendingUploads(): List<Word> {
        val ids = queries.getWordUploadQueue().awaitAsList()
        if (ids.isEmpty()) return emptyList()
        val tagMappings = queries.getTagMappingsForWords(ids).awaitAsList().groupBy { it.wordId }
        return queries.getWordsByIds(ids).awaitAsList().map { entity ->
            entity.toDomain(Language.ENGLISH, tagMappings[entity.id]?.map { it.tagId } ?: emptyList())
        }
    }

    override suspend fun completeUpload(uploaded: List<Int>, serverIds: Map<Int, Int>) {
        queries.transaction {
            if (uploaded.isNotEmpty()) queries.removeWordUploads(uploaded.map { it.toLong() })
            // Where each word sits now: a word may be moved aside before its own move comes up.
            val location = mutableMapOf<Long, Long>()
            serverIds.forEach { (local, server) ->
                val oldId = location[local.toLong()] ?: local.toLong()
                val newId = server.toLong()
                if (oldId == newId) return@forEach
                val moving = queries.getWordById(oldId).awaitAsOneOrNull() ?: return@forEach
                val occupant = queries.getWordById(newId).awaitAsOneOrNull()
                when {
                    occupant == null -> moveWordRow(oldId, newId)
                    // Already pulled from the server under its own id: this row is a duplicate.
                    occupant.toDomain().identity == moving.toDomain().identity -> dropDuplicateWordRow(oldId, newId)
                    else -> {
                        // An unrelated word whose device-local id is this server id: move it aside.
                        val freeId = (queries.maxWordId().awaitAsOne().MAX ?: newId) + 1
                        moveWordRow(newId, freeId)
                        location[newId] = freeId
                        moveWordRow(oldId, newId)
                    }
                }
            }
        }
    }

    /** Moves a word row with its tags, pending review and upload entry. Caller holds a transaction. */
    private suspend fun moveWordRow(oldId: Long, newId: Long) {
        queries.moveWordTags(newId = newId, oldId = oldId)
        queries.deleteWordTagsForWord(oldId)
        queries.moveReviewSyncEntry(newId = newId, oldId = oldId)
        queries.removeReviewSyncEntry(oldId)
        queries.moveWordUpload(newId = newId, oldId = oldId)
        queries.removeWordUploads(listOf(oldId))
        queries.changeWordId(newId = newId, oldId = oldId)
    }

    private suspend fun dropDuplicateWordRow(oldId: Long, keptId: Long) {
        queries.moveWordTags(newId = keptId, oldId = oldId)
        queries.moveReviewSyncEntry(newId = keptId, oldId = oldId)
        queries.removeReviewSyncEntry(oldId)
        queries.removeWordUploads(listOf(oldId))
        queries.deleteWord(oldId)
    }
}
