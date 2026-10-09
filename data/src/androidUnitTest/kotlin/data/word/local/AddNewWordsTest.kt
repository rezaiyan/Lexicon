package data.word.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import data.core.database.LexiconDatabase
import data.core.database.LexiconQueries
import data.tag.local.TagLocalDataSource
import domain.word.add.model.AddWordsOutcome
import domain.word.model.Word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** [WordLocalDataSource.addNewWords] against a real SQLite schema. */
class AddNewWordsTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var queries: LexiconQueries
    private lateinit var words: WordLocalDataSource
    private lateinit var tags: TagLocalDataSource

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LexiconDatabase.Schema.synchronous().create(driver)
        queries = LexiconDatabase(driver).lexiconQueries
        words = WordLocalDataSource(queries)
        tags = TagLocalDataSource(queries)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun card(
        term: String,
        translation: String,
        learning: Language = Language.GERMAN,
        tagIds: List<Long> = emptyList(),
    ) =
        Word.newCard(term, translation, "", learning, Language.ENGLISH, tagIds, nowMillis = 1L)

    @Test
    fun `addNewWords stores new words and reports the terms added`() = runTest {
        val outcome = words.addNewWords(listOf(card("Hund", "dog"), card("Katze", "cat")))

        assertEquals(AddWordsOutcome(2, 0, listOf("Hund", "Katze")), outcome)
        assertEquals(2, queries.getWordIdentitiesForLanguage(Language.GERMAN.code).executeAsList().size)
    }

    @Test
    fun `addNewWords skips words already stored ignoring case and spaces`() = runTest {
        words.addNewWords(listOf(card("Hund", "dog")))

        val outcome = words.addNewWords(listOf(card(" hund", "Dog "), card("Maus", "mouse")))

        assertEquals(AddWordsOutcome(1, 1, listOf("Maus")), outcome)
    }

    @Test
    fun `addNewWords treats the same word in another learning language as new`() = runTest {
        words.addNewWords(listOf(card("chat", "cat", learning = Language.FRENCH)))

        val outcome = words.addNewWords(listOf(card("chat", "cat", learning = Language.GERMAN)))

        assertEquals(1, outcome.added)
    }

    @Test
    fun `addNewWords links tags and queues every added word for upload`() = runTest {
        tags.insertOrReplaceTag(TAG_ID, "travel", 0, 0)

        words.addNewWords(listOf(card("Hund", "dog", tagIds = listOf(TAG_ID)), card("Katze", "cat")))

        assertEquals(1L, tags.getTags().first().single().wordCount)
        assertEquals(listOf("Hund", "Katze"), words.getPendingUploads().map { it.originalWord }.sorted())
    }

    @Test
    fun `deleting words that were never uploaded also drops them from the upload queue`() = runTest {
        words.addNewWords(listOf(card("Hund", "dog"), card("Katze", "cat"), card("Maus", "mouse")))
        val (first, second, third) = queries.getWordUploadQueue().awaitAsList()

        words.deleteWord(first.toInt())
        words.deleteWords(listOf(second.toInt()))

        assertEquals(listOf(third), queries.getWordUploadQueue().awaitAsList())
    }

    @Test
    fun `completeUpload moves uploaded words to their server ids with tags and pending reviews`() = runTest {
        tags.insertOrReplaceTag(TAG_ID, "travel", 0, 0)
        words.addNewWords(listOf(card("Hund", "dog", tagIds = listOf(TAG_ID)), card("Katze", "cat")))
        val (hund, katze) = queries.getWordUploadQueue().awaitAsList()
        queries.insertReviewSyncEntry(hund)

        words.completeUpload(uploaded = listOf(hund.toInt(), katze.toInt()), serverIds = mapOf(hund.toInt() to 500))

        assertEquals(emptyList(), queries.getWordUploadQueue().awaitAsList())
        assertEquals("Hund", words.getWordById(500)?.originalWord)
        assertEquals(listOf(TAG_ID), words.getWordById(500)?.tagIds)
        assertEquals(null, words.getWordById(hund.toInt()))
        assertEquals("Katze", words.getWordById(katze.toInt())?.originalWord)
        assertEquals(listOf(500L), queries.getAllReviewSyncEntries().awaitAsList())
    }

    @Test
    fun `completeUpload drops the local copy when the server word is already stored`() = runTest {
        words.addNewWords(listOf(card("Hund", "dog")))
        val local = queries.getWordUploadQueue().awaitAsList().single()
        // The same word already pulled from the server under its server id.
        words.insertWords(listOf(card("Hund", "dog").copy(id = 500)))

        words.completeUpload(uploaded = listOf(local.toInt()), serverIds = mapOf(local.toInt() to 500))

        assertEquals(null, words.getWordById(local.toInt()))
        assertEquals("Hund", words.getWordById(500)?.originalWord)
    }

    @Test
    fun `completeUpload moves an unrelated word holding the server id out of the way`() = runTest {
        tags.insertOrReplaceTag(TAG_ID, "travel", 0, 0)
        // A word added before re-keying existed: its local id happens to be another word's server id.
        words.insertWords(listOf(card("Maus", "mouse", tagIds = listOf(TAG_ID)).copy(id = 500)))
        queries.insertReviewSyncEntry(500)
        words.addNewWords(listOf(card("Hund", "dog")))
        val hund = queries.getWordUploadQueue().awaitAsList().single()

        words.completeUpload(uploaded = listOf(hund.toInt()), serverIds = mapOf(hund.toInt() to 500))

        assertEquals("Hund", words.getWordById(500)?.originalWord)
        val maus = words.getAllWordsAsync().single { it.originalWord == "Maus" }
        assertEquals(listOf(TAG_ID), maus.tagIds)
        assertEquals(listOf(maus.id.toLong()), queries.getAllReviewSyncEntries().awaitAsList())
    }

    private companion object {
        const val TAG_ID = 7L
    }
}
