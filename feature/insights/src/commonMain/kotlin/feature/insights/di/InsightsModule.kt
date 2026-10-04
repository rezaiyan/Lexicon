package feature.insights.di

import data.storage.DailyInsightCache
import domain.settings.usecase.ObserveReviewRemindersEnabledUseCase
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import feature.insights.InsightsUseCases
import feature.insights.InsightsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun insightsModule() = module {
    factoryOf(::GetWordRushInsightsUseCase)
    factoryOf(::ObserveReviewRemindersEnabledUseCase)
    factoryOf(::InsightsUseCases)

    viewModel {
        InsightsViewModel(
            useCases = get(),
            getWordRushInsightsUseCase = get(),
            getProfileStatsUseCase = get(),
            dailyInsightCache = get<DailyInsightCache>(),
            setReviewRemindersEnabledUseCase = get(),
            observeReviewRemindersEnabledUseCase = get(),
        )
    }
}
