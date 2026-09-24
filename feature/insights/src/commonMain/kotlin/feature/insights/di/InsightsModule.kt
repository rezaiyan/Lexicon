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

    factory {
        InsightsUseCases(
            getStudyInsights = get(),
            getDifficultWords = get(),
            getAccuracyTrend = get(),
            getAccuracyByLevel = get(),
            getStudyHeatmap = get(),
            getBestStudyTime = get(),
            getWordRushInsights = get(),
            getWeeklyReport = get(),
            getLevelTransitions = get(),
            getResponseTimeTrend = get(),
            getProfileStats = get(),
        )
    }

    viewModel {
        InsightsViewModel(
            useCases = get(),
            dailyInsightCache = get<DailyInsightCache>(),
            setReviewRemindersEnabledUseCase = get(),
            observeReviewRemindersEnabledUseCase = get(),
        )
    }
}
