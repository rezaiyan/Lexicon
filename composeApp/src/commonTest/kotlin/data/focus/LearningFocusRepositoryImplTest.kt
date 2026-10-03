package data.focus

import core.common.getOrThrow
import data.focus.local.ILearningFocusLocalDataSource
import data.focus.local.LearningFocusRecord
import data.focus.repository.LearningFocusRepositoryImpl
import domain.focus.model.LearningFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningFocusRepositoryImplTest {

    private class FakeLocal : ILearningFocusLocalDataSource {
        val record = MutableStateFlow<LearningFocusRecord?>(null)
        private fun current() = record.value ?: LearningFocusRecord(null, null, false)
        override fun observe(): Flow<LearningFocusRecord?> = record
        override suspend fun setFocusCode(code: String) {
            record.value = current().copy(focusCode = code)
        }
        override suspend fun setNudgeDismissedDay(day: Long) {
            record.value = current().copy(nudgeDismissedDay = day)
        }
        override suspend fun setIntroAcknowledged() {
            record.value = current().copy(introAcknowledged = true)
        }
    }

    private val local = FakeLocal()
    private val repo = LearningFocusRepositoryImpl(local)

    @Test
    fun `preference is null and intro not acknowledged when nothing stored`() = runTest {
        assertNull(repo.observePreference().first())
        assertNull(repo.observeNudgeDismissedDay().first())
        assertFalse(repo.observeIntroAcknowledged().first())
    }

    @Test
    fun `Single preference round trips through language code`() = runTest {
        repo.setPreference(LearningFocus.Single(Language.SPANISH)).getOrThrow()
        assertEquals("es", local.record.value?.focusCode)
        assertEquals(LearningFocus.Single(Language.SPANISH), repo.observePreference().first())
    }

    @Test
    fun `All preference round trips`() = runTest {
        repo.setPreference(LearningFocus.All).getOrThrow()
        assertEquals(LearningFocus.All, repo.observePreference().first())
    }

    @Test
    fun `nudge day and intro flag persist`() = runTest {
        repo.dismissNudge(20_000L).getOrThrow()
        repo.acknowledgeIntro().getOrThrow()
        assertEquals(20_000L, repo.observeNudgeDismissedDay().first())
        assertTrue(repo.observeIntroAcknowledged().first())
    }
}
