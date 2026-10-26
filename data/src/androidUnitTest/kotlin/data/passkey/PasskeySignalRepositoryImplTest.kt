package data.passkey

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import core.common.getOrThrow
import data.core.database.LexiconDatabase
import data.core.database.LexiconQueries
import data.settings.local.SettingsLocalDataSourceImpl
import domain.passkey.model.PasskeyAccountName
import domain.passkey.model.PasskeySignalsSent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [PasskeySignalRepositoryImpl] against a real SQLite schema. */
class PasskeySignalRepositoryImplTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var queries: LexiconQueries
    private lateinit var repository: PasskeySignalRepositoryImpl

    private val sent = PasskeySignalsSent(
        rpId = "lexicon.alirezaiyan.com",
        userHandle = "dXNlcg",
        credentialIds = setOf("Y3JlZC0y", "Y3JlZC0x"),
        accountName = PasskeyAccountName(name = "pat@gmail.com", displayName = "Pat"),
    )

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LexiconDatabase.Schema.synchronous().create(driver)
        queries = LexiconDatabase(driver).lexiconQueries
        repository = PasskeySignalRepositoryImpl(queries)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `lastSent before anything was saved is null`() = runTest {
        assertNull(repository.lastSent().getOrThrow())
    }

    @Test
    fun `save then lastSent returns what was saved`() = runTest {
        repository.save(sent).getOrThrow()

        assertEquals(sent, repository.lastSent().getOrThrow())
    }

    @Test
    fun `signals that never went through read back as null`() = runTest {
        val nothingTaken = sent.copy(credentialIds = null, accountName = null)

        repository.save(nothingTaken).getOrThrow()

        assertEquals(nothingTaken, repository.lastSent().getOrThrow())
    }

    @Test
    fun `save replaces the previous state`() = runTest {
        repository.save(sent).getOrThrow()
        val otherAccount = sent.copy(userHandle = "b3RoZXI", credentialIds = setOf("Y3JlZC0z"))

        repository.save(otherAccount).getOrThrow()

        assertEquals(otherAccount, repository.lastSent().getOrThrow())
    }

    @Test
    fun `signing out forgets what was sent`() = runTest {
        repository.save(sent).getOrThrow()

        SettingsLocalDataSourceImpl(queries).clearSettings()

        assertNull(repository.lastSent().getOrThrow())
    }
}
