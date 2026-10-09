package data.word.add

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import core.common.getOrThrow
import data.core.database.LexiconDatabase
import data.core.database.LexiconQueries
import data.settings.local.SettingsLocalDataSourceImpl
import data.word.local.WordLocalDataSource
import domain.word.add.model.LanguagePair
import domain.word.model.Word
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [AddWordsLanguageRepositoryImpl] against a real SQLite schema. */
class AddWordsLanguageRepositoryImplTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var queries: LexiconQueries
    private lateinit var repository: AddWordsLanguageRepositoryImpl

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LexiconDatabase.Schema.synchronous().create(driver)
        queries = LexiconDatabase(driver).lexiconQueries
        repository = AddWordsLanguageRepositoryImpl(queries)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private suspend fun storeWords(learning: Language, native: Language, count: Int) {
        val cards = (1..count).map { Word.newCard("$learning$it", "t$it", "", learning, native, emptyList(), 1L) }
        WordLocalDataSource(queries).addNewWords(cards)
    }

    @Test
    fun `lastUsed when nothing saved returns null`() = runTest {
        assertNull(repository.lastUsed().getOrThrow())
    }

    @Test
    fun `saveLastUsed then lastUsed returns the saved pair`() = runTest {
        repository.saveLastUsed(LanguagePair(Language.GERMAN, Language.ENGLISH))
        repository.saveLastUsed(LanguagePair(Language.FRENCH, Language.PERSIAN))

        assertEquals(LanguagePair(Language.FRENCH, Language.PERSIAN), repository.lastUsed().getOrThrow())
    }

    @Test
    fun `lastUsed when a stored code is unknown returns null instead of English`() = runTest {
        queries.saveAddWordsPreference("xx", Language.ENGLISH.code)

        assertNull(repository.lastUsed().getOrThrow())
    }

    @Test
    fun `clearing settings on sign-out forgets the last used pair`() = runTest {
        repository.saveLastUsed(LanguagePair(Language.GERMAN, Language.ENGLISH))

        SettingsLocalDataSourceImpl(queries).clearSettings()

        assertNull(repository.lastUsed().getOrThrow())
    }

    @Test
    fun `mostCommon returns the pair used by most words`() = runTest {
        storeWords(Language.GERMAN, Language.ENGLISH, count = 3)
        storeWords(Language.FRENCH, Language.ENGLISH, count = 1)

        assertEquals(LanguagePair(Language.GERMAN, Language.ENGLISH), repository.mostCommon().getOrThrow())
    }

    @Test
    fun `mostCommon with no words returns null`() = runTest {
        assertNull(repository.mostCommon().getOrThrow())
    }
}
