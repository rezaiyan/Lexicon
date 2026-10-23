package domain.analytics.usecase

import core.common.Try
import core.common.getOrThrow
import domain.analytics.model.ReviewEventParams
import domain.analytics.repository.IAnalyticsRecorder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsUseCaseTests {

    // region Fakes

    private class FakeAnalyticsRecorder : IAnalyticsRecorder {
        var startSessionResult: Try<Unit> = Try.success(Unit)
        var endSessionResult: Try<Unit> = Try.success(Unit)
        var recordReviewEventResult: Try<Unit> = Try.success(Unit)
        var retryPendingSyncResult: Try<Unit> = Try.success(Unit)

        var lastStartSessionId: String? = null
        var lastStartReviewType: String? = null
        var lastEndSessionId: String? = null
        var lastRecordSessionId: String? = null
        var lastRecordWordId: Int? = null

        override suspend fun startSession(
            sessionId: String,
            reviewType: String,
            startedAt: Long,
        ): Try<Unit> {
            lastStartSessionId = sessionId
            lastStartReviewType = reviewType
            return startSessionResult
        }

        override suspend fun endSession(
            sessionId: String,
            endedAt: Long,
            durationMs: Long,
            totalCards: Int,
            correctCount: Int,
            incorrectCount: Int,
            completedNormally: Boolean,
        ): Try<Unit> {
            lastEndSessionId = sessionId
            return endSessionResult
        }

        override suspend fun recordReviewEvent(params: ReviewEventParams): Try<Unit> {
            lastRecordSessionId = params.sessionId
            lastRecordWordId = params.wordId
            return recordReviewEventResult
        }

        override suspend fun retryPendingSync(): Try<Unit> = retryPendingSyncResult
    }

    // endregion

    // region StartStudySessionUseCase

    @Test
    fun `StartStudySessionUseCase - success returns sessionId`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = StartStudySessionUseCase(recorder)

        val result = useCase(StartStudySessionUseCase.Params(sessionId = "session-1", reviewType = "flashcard"))

        assertTrue(result.isSuccess)
        assertEquals("session-1", result.getOrThrow())
        assertEquals("session-1", recorder.lastStartSessionId)
        assertEquals("flashcard", recorder.lastStartReviewType)
    }

    @Test
    fun `StartStudySessionUseCase - failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.startSessionResult = Try.failure(RuntimeException("DB error"))
        val useCase = StartStudySessionUseCase(recorder)

        val result = useCase(StartStudySessionUseCase.Params(sessionId = "session-1", reviewType = "flashcard"))

        assertTrue(result.isFailure)
    }

    // endregion

    // region EndStudySessionUseCase

    @Test
    fun `EndStudySessionUseCase - success ends session`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = EndStudySessionUseCase(recorder)

        val result = useCase(
            EndStudySessionUseCase.Params(
                sessionId = "session-1",
                endedAt = 1000L,
                durationMs = 500L,
                totalCards = 10,
                correctCount = 8,
                incorrectCount = 2,
                completedNormally = true,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("session-1", recorder.lastEndSessionId)
    }

    @Test
    fun `EndStudySessionUseCase - recorder failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.endSessionResult = Try.failure(RuntimeException("DB error"))
        val useCase = EndStudySessionUseCase(recorder)

        val result = useCase(
            EndStudySessionUseCase.Params(
                sessionId = "session-1",
                endedAt = 1000L,
                durationMs = 500L,
                totalCards = 10,
                correctCount = 8,
                incorrectCount = 2,
                completedNormally = true,
            )
        )

        assertTrue(result.isFailure)
    }

    // endregion

    // region RecordReviewEventUseCase

    @Test
    fun `RecordReviewEventUseCase - success records event`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = RecordReviewEventUseCase(recorder)

        val result = useCase(
            ReviewEventParams(
                sessionId = "session-1",
                wordId = 42,
                wordText = "hello",
                wordTranslation = "hola",
                sourceLanguage = "EN",
                targetLanguage = "ES",
                rating = 4,
                previousLevel = 2,
                newLevel = 3,
                responseTimeMs = 1500,
                reviewedAt = 1000L,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("session-1", recorder.lastRecordSessionId)
        assertEquals(42, recorder.lastRecordWordId)
    }

    @Test
    fun `RecordReviewEventUseCase - failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.recordReviewEventResult = Try.failure(RuntimeException("DB error"))
        val useCase = RecordReviewEventUseCase(recorder)

        val result = useCase(
            ReviewEventParams(
                sessionId = "session-1",
                wordId = 42,
                wordText = "hello",
                wordTranslation = "hola",
                sourceLanguage = "EN",
                targetLanguage = "ES",
                rating = 4,
                previousLevel = 2,
                newLevel = 3,
                responseTimeMs = 1500,
                reviewedAt = 1000L,
            )
        )

        assertTrue(result.isFailure)
    }

    // endregion

    // region RetryAnalyticsSyncUseCase

    @Test
    fun `RetryAnalyticsSyncUseCase - success delegates to recorder`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = RetryAnalyticsSyncUseCase(recorder)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `RetryAnalyticsSyncUseCase - failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.retryPendingSyncResult = Try.failure(RuntimeException("Sync error"))
        val useCase = RetryAnalyticsSyncUseCase(recorder)

        val result = useCase(Unit)

        assertTrue(result.isFailure)
    }

    // endregion
}
