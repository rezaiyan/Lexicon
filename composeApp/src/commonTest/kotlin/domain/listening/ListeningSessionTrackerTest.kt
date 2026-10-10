package domain.listening

import domain.listening.model.HeardWord
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSessionTracker
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.listening.usecase.RecordListeningSessionUseCase
import domain.word.model.Word
import fakes.FakeListeningRecorder
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListeningSessionTrackerTest {

    private val start = 1_000L

    private fun tracker() = ListeningSessionTracker(
        clientSessionId = "s-1",
        selection = ListeningSelection(source = ListeningSource.Tag(7)),
        settings = ListeningSettings(pauseMs = 2_000, order = ListeningOrder.TRANSLATION_FIRST, repeatCount = 2),
        speechRate = 0.8f,
        plannedWords = 3,
        startedAt = start,
    )

    private fun word(id: Int) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )

    @Test
    fun `end without pauses counts the whole session as listening time`() {
        val record = tracker().end(at = 11_000, completedNormally = true)

        assertEquals(10_000, record.durationMs)
        assertEquals(10_000, record.listeningMs)
        assertEquals(start, record.startedAt)
        assertEquals(11_000, record.endedAt)
        assertTrue(record.completedNormally)
    }

    @Test
    fun `paused time is excluded from listening time`() {
        val record = tracker()
            .paused(at = 3_000)
            .resumed(at = 8_000)
            .end(at = 10_000, completedNormally = false)

        assertEquals(9_000, record.durationMs)
        assertEquals(4_000, record.listeningMs) // 1s–3s and 8s–10s
        assertEquals(1, record.pauseCount)
    }

    @Test
    fun `ending while paused stops listening time at the pause`() {
        val record = tracker().paused(at = 4_000).end(at = 20_000, completedNormally = false)

        assertEquals(3_000, record.listeningMs)
    }

    @Test
    fun `repeated pause or resume events are ignored`() {
        val record = tracker()
            .paused(at = 2_000)
            .paused(at = 3_000)
            .resumed(at = 5_000)
            .resumed(at = 6_000)
            .end(at = 7_000, completedNormally = true)

        assertEquals(1, record.pauseCount)
        assertEquals(3_000, record.listeningMs)
    }

    @Test
    fun `a word heard on repeat is counted once at its first time`() {
        val record = tracker()
            .heard(word(1), at = 2_000)
            .heard(word(1), at = 4_000)
            .heard(word(2), at = 6_000)
            .end(at = 7_000, completedNormally = true)

        assertEquals(
            listOf(HeardWord(1, "en", "de", 2_000), HeardWord(2, "en", "de", 6_000)),
            record.heardWords,
        )
    }

    @Test
    fun `record carries the settings and selection the session started with`() {
        val record = tracker().skipped().skipped().end(at = 2_000, completedNormally = true)

        assertEquals(ListeningSource.Tag(7), record.selection.source)
        assertEquals(ListeningOrder.TRANSLATION_FIRST, record.order)
        assertEquals(2, record.repeatCount)
        assertEquals(2_000, record.pauseMs)
        assertEquals(0.8f, record.speechRate)
        assertEquals(3, record.plannedWords)
        assertEquals(2, record.wordsSkipped)
    }

    @Test
    fun `use case drops sessions where nothing was heard`() = runTest {
        val recorder = FakeListeningRecorder()
        val useCase = RecordListeningSessionUseCase(recorder)

        useCase(tracker().end(at = 2_000, completedNormally = false))
        useCase(tracker().heard(word(1), at = 1_500).end(at = 2_000, completedNormally = false))

        assertEquals(1, recorder.recorded.size)
        assertEquals(1, recorder.recorded.single().heardWords.size)
    }
}
