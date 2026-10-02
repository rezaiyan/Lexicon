package data.word.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import data.core.database.LexiconDatabase
import data.core.database.LexiconQueries
import data.tag.local.TagLocalDataSource
import fakes.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression: deleting words left their WordTagEntity rows behind, so tag word counts
 * (Study tag lists, review selector) never dropped while stage counts did.
 */
class WordDeletionTagCountTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var queries: LexiconQueries
    private lateinit var words: WordLocalDataSource
    private lateinit var tags: TagLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LexiconDatabase.Schema.synchronous().create(driver)
        queries = LexiconDatabase(driver).lexiconQueries
        words = WordLocalDataSource(queries, FakeSettingsRepository())
        tags = TagLocalDataSource(queries)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `deleteWords when words are tagged drops them from the tag word count`() = runTest {
        val ids = (1..3).map { insertWord("w$it") }
        tagWords(TAG_ID, ids)

        words.deleteWords(ids.take(2).map(Long::toInt))

        assertEquals(1L, tags.getTags().first().single().wordCount)
    }

    @Test
    fun `deleteWord when word is tagged drops it from the tag word count`() = runTest {
        val ids = (1..2).map { insertWord("w$it") }
        tagWords(TAG_ID, ids)

        words.deleteWord(ids.first().toInt())

        assertEquals(1L, tags.getTags().first().single().wordCount)
    }

    @Test
    fun `deleteAllWords when words are tagged leaves every tag with zero words`() = runTest {
        val ids = (1..2).map { insertWord("w$it") }
        tagWords(TAG_ID, ids)

        words.deleteAllWords()

        assertEquals(0L, tags.getTags().first().single().wordCount)
    }

    @Test
    fun `getTags when a tag link points at a missing word does not count it`() = runTest {
        val id = insertWord("kept")
        tagWords(TAG_ID, listOf(id, ORPHAN_WORD_ID))

        assertEquals(1L, tags.getTags().first().single().wordCount)
    }

    @Test
    fun `getTags flow when tagged words are deleted re-emits the lower count`() = runTest {
        val ids = (1..2).map { insertWord("w$it") }
        tagWords(TAG_ID, ids)

        withContext(Dispatchers.Default) {
            val dropped = async {
                withTimeout(5_000) { tags.getTags().first { it.single().wordCount == 0L } }
            }
            words.deleteWords(ids.map(Long::toInt))
            assertEquals(0L, dropped.await().single().wordCount)
        }
    }

    private suspend fun insertWord(text: String): Long {
        queries.insertWord(text, "t", "", "en", "de", 0, 2.5, 0, 0, 0, 0, 0)
        return queries.lastInsertRowId().executeAsOne()
    }

    private suspend fun tagWords(tagId: Long, wordIds: List<Long>) {
        tags.insertOrReplaceTag(tagId, "travel", 0, 0)
        wordIds.forEach { queries.insertWordTag(it, tagId) }
    }

    private companion object {
        const val TAG_ID = 7L
        const val ORPHAN_WORD_ID = 9_999L
    }
}
