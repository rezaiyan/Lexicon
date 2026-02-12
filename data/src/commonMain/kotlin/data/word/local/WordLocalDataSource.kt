package data.word.local

import data.core.database.ProgressRow
import data.core.database.LexiconDao
import data.core.database.WordEntity
import data.word.mapper.WordMapper
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    private val dao: LexiconDao
) : IWordLocalDataSource {

    override suspend fun getAllWordsAsync(): List<Word> {
        return WordMapper.toDomainList(dao.getAllAsync())
    }

    override fun getAllWords(): Flow<List<Word>> {
        return dao.getAll().map { WordMapper.toDomainList(it) }
    }

    override fun getDueCards(): Flow<List<Word>> {
        return dao.getDueCards().map { WordMapper.toDomainList(it) }
    }

    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> {
        return dao.getWordsByLevel(stage.level).map { WordMapper.toDomainList(it) }
    }

    override suspend fun getWordById(id: Int): Word? {
        return dao.getWordById(id.toLong())?.let { WordMapper.toDomain(it) }
    }

    override suspend fun insertWords(words: List<Word>) {
        dao.insert(WordMapper.toEntityList(words))
    }

    override suspend fun updateWord(word: Word) {
        dao.upsert(WordMapper.toEntity(word))
    }

    override suspend fun deleteWord(id: Int) {
        dao.deleteWord(id.toLong())
    }

    override suspend fun deleteWords(ids: List<Int>): Int {
        val longIds = ids.map { it.toLong() }
        return dao.deleteWords(longIds)
    }

    override suspend fun getAllWordsOnce(): List<WordEntity> {
        return dao.getAllOnce()
    }

    override fun getProgressStats(currentTime: Long): Flow<ProgressStats> {
        return dao.progressRowFlow(currentTime).map { mapProgressRow(it) }
    }

    override suspend fun getTotalCount(): Int {
        return dao.count()
    }

    override suspend fun getDueCount(): Int {
        return dao.countDueCards()
    }

    override suspend fun deleteAllWords() {
        dao.deleteAllWords()
    }

    private fun mapProgressRow(progressRow: ProgressRow): ProgressStats {
        return ProgressStats(
            level0Count = progressRow.level0Count,
            level1Count = progressRow.level1Count,
            level2Count = progressRow.level2Count,
            level3Count = progressRow.level3Count,
            level4Count = progressRow.level4Count,
            level5Count = progressRow.level5Count,
            level6Count = progressRow.level6Count,
            totalWords = progressRow.totalWords,
            dueCards = progressRow.dueCards
        )
    }
}

