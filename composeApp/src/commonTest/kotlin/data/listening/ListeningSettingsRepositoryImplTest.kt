package data.listening

import core.common.getOrThrow
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.word.model.LearningStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListeningSettingsRepositoryImplTest {

    private class FakeLocalDataSource : IListeningSettingsLocalDataSource {
        val record = MutableStateFlow<ListeningSettingsRecord?>(null)
        override fun observe(): Flow<ListeningSettingsRecord?> = record
        override suspend fun save(record: ListeningSettingsRecord) {
            this.record.value = record
        }
    }

    @Test
    fun `observe emits defaults when nothing is stored`() = runTest {
        val repo = ListeningSettingsRepositoryImpl(FakeLocalDataSource())

        assertEquals(ListeningSettings(), repo.observe().first())
    }

    @Test
    fun `save then observe round-trips every field`() = runTest {
        val repo = ListeningSettingsRepositoryImpl(FakeLocalDataSource())
        val settings = ListeningSettings(
            pauseMs = 5_000L,
            order = ListeningOrder.TRANSLATION_FIRST,
            repeatCount = 2,
            selection = ListeningSelection(ListeningSource.Tag(42L), limit = 30, shuffle = true),
        )

        repo.save(settings).getOrThrow()

        assertEquals(settings, repo.observe().first())
    }

    @Test
    fun `every listening source round-trips through its stored key`() {
        val sources = listOf(ListeningSource.Due, ListeningSource.All, ListeningSource.Tag(7L)) +
            LearningStage.entries.map(ListeningSource::Level)

        sources.forEach { assertEquals(it, it.toKey().toListeningSource()) }
    }

    @Test
    fun `malformed stored source falls back to due words`() {
        listOf("", "NOPE", "LEVEL:x", "LEVEL:99", "TAG:", "TAG:abc").forEach {
            assertEquals(ListeningSource.Due, it.toListeningSource())
        }
    }

    @Test
    fun `unknown stored order falls back to word first`() = runTest {
        val local = FakeLocalDataSource().apply { record.value = ListeningSettingsRecord(2_000L, "SIDEWAYS", 1L, "DUE", 20L, false) }

        assertEquals(ListeningOrder.WORD_FIRST, ListeningSettingsRepositoryImpl(local).observe().first().order)
    }
}
