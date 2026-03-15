package fakes

import core.common.Try
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository
import domain.widget.IWidgetRefresher
import domain.widget.model.DailyWidgetData
import domain.widget.usecase.GetDailyWidgetDataUseCase
import domain.word.repository.IWordRepository

class FakeWidgetRefresher : IWidgetRefresher {
    var pushedData: DailyWidgetData? = null
        private set

    private var displayedWordId: Int? = null

    override suspend fun getDisplayedWordId(): Int? = displayedWordId

    override suspend fun push(data: DailyWidgetData) {
        displayedWordId = data.wordId
        pushedData = data
    }
}

fun fakeGetDailyWidgetDataUseCase(
    wordRepository: IWordRepository,
): GetDailyWidgetDataUseCase {
    val noOpStreakRepo = object : IStreakRepository {
        override suspend fun getStreak(): Try<StreakData> = Try.success(StreakData(0))
        override suspend fun recordActivity(count: Int): Try<StreakData> = Try.success(StreakData(0))
    }
    return GetDailyWidgetDataUseCase(wordRepository, noOpStreakRepo, FakeWidgetRefresher())
}
