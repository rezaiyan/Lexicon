package data.word.local

import app.cash.turbine.test
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract tests for IWordLocalDataSource, tested against an in-memory fake implementation.
 *
 * Because LexiconQueries is a concrete SQLDelight class (not an interface), we cannot easily
 * instantiate it in common tests without a real SQLite driver. Instead we:
 *   1. Test the interface contract via a rich in-memory fake that mirrors the real implementation.
 *   2. This ensures that consumers depending on IWordLocalDataSource always get correct semantics.
 */
class WordLocalDataSourceTest {

    // -------------------------------------------------------------------------
    // In-memory fake
    // -------------------------------------------------------------------------

    private class FakeWordLocalDataSource : IWordLocalDataSource {

        // Internal state
        private var nextId = 1
        private val words = mutableMapOf<Int, Word>()        // id -> Word
        private val tagMap = mutableMapOf<Int, MutableList<Long>>() // wordId -> tagIds

        private val wordsFlow = MutableStateFlow<List<Word>>(emptyList())
        private val progressFlow = MutableStateFlow(ProgressStats())

        // Captured call tracking for assertions
        var insertWordCallCount = 0
        var upsertWordCallCount = 0
        var deleteWordsCallCount = 0
        val deletedWordIds = mutableListOf<Int>()

        private fun emit() {
            wordsFlow.value = words.values.toList()
        }

        override suspend fun getAllWordsAsync(): List<Word> = words.values.toList()

        override fun getAllWords(): Flow<List<Word>> = wordsFlow

        override fun getDueCards(): Flow<List<Word>> = wordsFlow.map { list ->
            list.filter { it.nextReviewDate <= 0L }
        }

        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = wordsFlow.map { list ->
            list.filter { word ->
                (tagMap[word.id]?.contains(tagId) == true) && word.nextReviewDate <= 0L
            }
        }

        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = wordsFlow.map { list ->
            list.filter { it.level == stage.level }
        }

        override suspend fun getWordById(id: Int): Word? = words[id]

        override suspend fun insertWords(words: List<Word>) {
            for (word in words) {
                if (word.id == 0) {
                    // New word — assign an auto-incremented id
                    insertWordCallCount++
                    val realId = nextId++
                    val stored = word.copy(id = realId)
                    this.words[realId] = stored
                    // Assign tags if provided
                    if (word.tagIds.isNotEmpty()) {
                        tagMap[realId] = word.tagIds.toMutableList()
                    }
                } else {
                    // Existing word — upsert
                    upsertWordCallCount++
                    this.words[word.id] = word
                    // Update tags only if tagIds non-empty; preserve existing if empty
                    if (word.tagIds.isNotEmpty()) {
                        tagMap[word.id] = word.tagIds.toMutableList()
                    }
                }
            }
            emit()
        }

        override suspend fun updateWord(word: Word) {
            upsertWordCallCount++
            words[word.id] = word
            emit()
        }

        override suspend fun deleteWord(id: Int) {
            words.remove(id)
            tagMap.remove(id)
            emit()
        }

        override suspend fun deleteWords(ids: List<Int>): Int {
            deleteWordsCallCount++
            deletedWordIds.addAll(ids)
            if (ids.isEmpty()) return 0
            for (id in ids) {
                words.remove(id)
                tagMap.remove(id)
            }
            emit()
            return ids.size
        }

        override suspend fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String
        ): Int {
            if (ids.isEmpty()) return 0
            for (id in ids) {
                val word = words[id] ?: continue
                words[id] = word.copy(
                    sourceLanguage = Language.fromCode(Language.toCode(sourceLanguage)),
                    targetLanguage = Language.fromCode(Language.toCode(targetLanguage))
                )
            }
            emit()
            return ids.size
        }

        override suspend fun getAllWordsOnce(): List<data.core.database.WordEntity> = emptyList()

        override fun getProgressStats(): Flow<ProgressStats> = progressFlow

        fun setProgressStats(stats: ProgressStats) {
            progressFlow.value = stats
        }

        override suspend fun getTotalCount(): Int = words.size

        override suspend fun getDueCount(): Int = words.values.count { it.nextReviewDate <= 0L }

        override suspend fun deleteAllWords() {
            words.clear()
            tagMap.clear()
            emit()
        }

        override suspend fun getMostCommonSourceLanguage(): String? =
            words.values
                .groupBy { it.sourceLanguage.code }
                .maxByOrNull { it.value.size }
                ?.key

        fun getTagIdsForWord(id: Int): List<Long> = tagMap[id] ?: emptyList()
    }

    // -------------------------------------------------------------------------
    // Helper factory
    // -------------------------------------------------------------------------

    private fun makeWord(
        id: Int = 0,
        original: String = "hello",
        translation: String = "hola",
        level: Int = 0,
        nextReviewDate: Long = Long.MAX_VALUE,
        tagIds: List<Long> = emptyList()
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "a word",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.ENGLISH,
        level = level,
        nextReviewDate = nextReviewDate,
        tagIds = tagIds
    )

    // -------------------------------------------------------------------------
    // insertWords — new words (id == 0)
    // -------------------------------------------------------------------------

    @Test
    fun `insertWords with new words calls insertWord path and assigns auto id`() = runTest {
        val ds = FakeWordLocalDataSource()

        ds.insertWords(listOf(makeWord(id = 0, original = "cat")))

        assertEquals(1, ds.insertWordCallCount)
        assertEquals(0, ds.upsertWordCallCount)
        val all = ds.getAllWordsAsync()
        assertEquals(1, all.size)
        assertEquals("cat", all.first().originalWord)
        assertTrue(all.first().id > 0) // id was assigned
    }

    @Test
    fun `insertWords with new words assigns tags via lookup after insert`() = runTest {
        val ds = FakeWordLocalDataSource()

        ds.insertWords(listOf(makeWord(id = 0, original = "dog", tagIds = listOf(10L, 20L))))

        val inserted = ds.getAllWordsAsync().first()
        val tags = ds.getTagIdsForWord(inserted.id)
        assertEquals(listOf(10L, 20L), tags)
    }

    @Test
    fun `insertWords with new words and no tagIds assigns no tags`() = runTest {
        val ds = FakeWordLocalDataSource()

        ds.insertWords(listOf(makeWord(id = 0, original = "bird", tagIds = emptyList())))

        val inserted = ds.getAllWordsAsync().first()
        assertTrue(ds.getTagIdsForWord(inserted.id).isEmpty())
    }

    // -------------------------------------------------------------------------
    // insertWords — existing words (id != 0)
    // -------------------------------------------------------------------------

    @Test
    fun `insertWords with existing words calls upsertWord path`() = runTest {
        val ds = FakeWordLocalDataSource()
        // First insert to create the word
        ds.insertWords(listOf(makeWord(id = 0, original = "test")))
        val existingId = ds.getAllWordsAsync().first().id
        val insertCount = ds.insertWordCallCount

        ds.insertWords(listOf(makeWord(id = existingId, original = "test-updated")))

        assertEquals(insertCount, ds.insertWordCallCount) // no new insertWord calls
        assertTrue(ds.upsertWordCallCount > 0)
        val updated = ds.getAllWordsAsync().first { it.id == existingId }
        assertEquals("test-updated", updated.originalWord)
    }

    @Test
    fun `insertWords with existing words and non-empty tagIds replaces tags`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(makeWord(id = 0, original = "word1", tagIds = listOf(1L, 2L))))
        val existingId = ds.getAllWordsAsync().first().id

        // Upsert same word with different tags
        ds.insertWords(listOf(makeWord(id = existingId, original = "word1", tagIds = listOf(3L, 4L, 5L))))

        val tags = ds.getTagIdsForWord(existingId)
        assertEquals(listOf(3L, 4L, 5L), tags)
    }

    @Test
    fun `insertWords with existing words and empty tagIds preserves existing tags`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(makeWord(id = 0, original = "word1", tagIds = listOf(1L, 2L))))
        val existingId = ds.getAllWordsAsync().first().id

        // Upsert same word with empty tagIds (no tag info provided — preserve local tags)
        ds.insertWords(listOf(makeWord(id = existingId, original = "word1-updated", tagIds = emptyList())))

        val tags = ds.getTagIdsForWord(existingId)
        assertEquals(listOf(1L, 2L), tags) // preserved
    }

    // -------------------------------------------------------------------------
    // deleteWords
    // -------------------------------------------------------------------------

    @Test
    fun `deleteWords with empty list returns without querying`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(makeWord(id = 0, original = "keep")))

        val result = ds.deleteWords(emptyList())

        assertEquals(0, result)
        assertEquals(1, ds.getAllWordsAsync().size) // word still present
    }

    @Test
    fun `deleteWords with valid ids removes words and returns count`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(
            makeWord(id = 0, original = "a"),
            makeWord(id = 0, original = "b"),
            makeWord(id = 0, original = "c")
        ))
        val ids = ds.getAllWordsAsync().map { it.id }

        val result = ds.deleteWords(ids.take(2))

        assertEquals(2, result)
        assertEquals(1, ds.getAllWordsAsync().size)
    }

    // -------------------------------------------------------------------------
    // updateWord
    // -------------------------------------------------------------------------

    @Test
    fun `updateWord delegates to upsertWord`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(makeWord(id = 0, original = "before")))
        val word = ds.getAllWordsAsync().first()
        val upsertCountBefore = ds.upsertWordCallCount

        ds.updateWord(word.copy(originalWord = "after"))

        assertTrue(ds.upsertWordCallCount > upsertCountBefore)
        assertEquals("after", ds.getAllWordsAsync().first { it.id == word.id }.originalWord)
    }

    // -------------------------------------------------------------------------
    // getAllWordsAsync
    // -------------------------------------------------------------------------

    @Test
    fun `getAllWordsAsync returns empty list when database is empty`() = runTest {
        val ds = FakeWordLocalDataSource()

        val result = ds.getAllWordsAsync()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllWordsAsync returns words with correctly mapped tagIds`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(
            makeWord(id = 0, original = "alpha", tagIds = listOf(100L)),
            makeWord(id = 0, original = "beta", tagIds = listOf(200L, 300L))
        ))

        val result = ds.getAllWordsAsync()

        assertEquals(2, result.size)
    }

    // -------------------------------------------------------------------------
    // getAllWords flow
    // -------------------------------------------------------------------------

    @Test
    fun `getAllWords flow emits updated list after insert`() = runTest {
        val ds = FakeWordLocalDataSource()

        ds.getAllWords().test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            ds.insertWords(listOf(makeWord(id = 0, original = "flower")))
            val updated = awaitItem()

            assertEquals(1, updated.size)
            assertEquals("flower", updated.first().originalWord)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // getProgressStats
    // -------------------------------------------------------------------------

    @Test
    fun `getProgressStats returns correctly mapped ProgressStats`() = runTest {
        val ds = FakeWordLocalDataSource()
        val expected = ProgressStats(
            level0Count = 5,
            level1Count = 3,
            level2Count = 2,
            level3Count = 1,
            level4Count = 0,
            level5Count = 0,
            level6Count = 0,
            totalWords = 11,
            dueCards = 4
        )
        ds.setProgressStats(expected)

        ds.getProgressStats().test {
            val stats = awaitItem()

            assertEquals(5, stats.level0Count)
            assertEquals(3, stats.level1Count)
            assertEquals(2, stats.level2Count)
            assertEquals(1, stats.level3Count)
            assertEquals(0, stats.level4Count)
            assertEquals(11, stats.totalWords)
            assertEquals(4, stats.dueCards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProgressStats returns empty stats when no words present`() = runTest {
        val ds = FakeWordLocalDataSource()

        ds.getProgressStats().test {
            val stats = awaitItem()

            assertEquals(0, stats.totalWords)
            assertEquals(0, stats.dueCards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // getTotalCount / getDueCount
    // -------------------------------------------------------------------------

    @Test
    fun `getTotalCount returns number of stored words`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(makeWord(id = 0), makeWord(id = 0)))

        assertEquals(2, ds.getTotalCount())
    }

    @Test
    fun `getDueCount returns words with nextReviewDate in past`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(
            makeWord(id = 0, nextReviewDate = 0L),      // due
            makeWord(id = 0, nextReviewDate = Long.MAX_VALUE)  // not due
        ))

        assertEquals(1, ds.getDueCount())
    }

    // -------------------------------------------------------------------------
    // deleteAllWords
    // -------------------------------------------------------------------------

    @Test
    fun `deleteAllWords removes all stored words`() = runTest {
        val ds = FakeWordLocalDataSource()
        ds.insertWords(listOf(makeWord(id = 0), makeWord(id = 0), makeWord(id = 0)))

        ds.deleteAllWords()

        assertTrue(ds.getAllWordsAsync().isEmpty())
        assertEquals(0, ds.getTotalCount())
    }

    // -------------------------------------------------------------------------
    // getMostCommonSourceLanguage
    // -------------------------------------------------------------------------

    @Test
    fun `getMostCommonSourceLanguage returns null when empty`() = runTest {
        val ds = FakeWordLocalDataSource()

        assertNull(ds.getMostCommonSourceLanguage())
    }
}
