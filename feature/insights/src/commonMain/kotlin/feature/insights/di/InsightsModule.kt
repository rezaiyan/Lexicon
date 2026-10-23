package feature.insights.di

import domain.word.usecase.ObserveReviewForecastUseCase
import feature.insights.coach.InsightsCoachUseCases
import feature.insights.coach.InsightsCoachViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import time.is24HourClock

fun insightsModule() = module {
    factory { ObserveReviewForecastUseCase(wordRepository = get()) }
    factoryOf(::InsightsCoachUseCases)

    viewModel {
        InsightsCoachViewModel(
            useCases = get(),
            analyticsTracker = get(),
            use24Hour = ::is24HourClock,
        )
    }
}
