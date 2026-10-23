package domain.word.usecase

import core.common.NoParamFlowUseCase
import domain.word.model.ReviewForecast
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** Upcoming review load per local day, from the words on this device (works offline). */
class ObserveReviewForecastUseCase(
    private val wordRepository: IWordRepository,
    private val now: () -> Instant = { Clock.System.now() },
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : NoParamFlowUseCase<ReviewForecast> {
    override operator fun invoke(params: Unit): Flow<ReviewForecast> =
        wordRepository.getAllWords().map { words ->
            ReviewForecast.of(words.map { it.nextReviewDate }, now(), timeZone())
        }
}
