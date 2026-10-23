package domain.word.usecase

import domain.word.model.Word
import fakes.FakeWordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveReviewForecastUseCaseTest {

    private val zone = TimeZone.of("Europe/Berlin")
    private val now = LocalDateTime(2026, 10, 23, 18, 0).toInstant(zone)
    private val repository = FakeWordRepository()
    private val useCase = ObserveReviewForecastUseCase(repository, now = { now }, timeZone = { zone })

    private fun at(day: Int, hour: Int) = LocalDateTime(2026, 10, day, hour, 0).toInstant(zone).toEpochMilliseconds()

    @Test
    fun `forecast when words are due counts overdue as due now and buckets the rest by local day`() = runTest {
        repository.storedWords = mutableListOf(
            word(1, due = at(20, 9)), // overdue
            word(2, due = at(23, 17)), // due earlier today
            word(3, due = at(23, 23)), // later today
            word(4, due = at(24, 0)), // tomorrow, just after midnight
            word(5, due = at(24, 12)),
            word(6, due = at(29, 8)), // last day of the window
            word(7, due = at(30, 8)), // beyond the window
        )

        val forecast = useCase(Unit).first()

        assertEquals(2, forecast.dueNow)
        assertEquals((23..29).map { LocalDate(2026, 10, it) }, forecast.days.map { it.date })
        assertEquals(listOf(3, 2, 0, 0, 0, 0, 1), forecast.days.map { it.reviews })
    }

    @Test
    fun `forecast when library is empty has seven empty days`() = runTest {
        val forecast = useCase(Unit).first()

        assertEquals(0, forecast.dueNow)
        assertEquals(7, forecast.days.size)
        assertEquals(0, forecast.days.sumOf { it.reviews })
    }

    private fun word(id: Int, due: Long) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = due,
    )
}
