package feature.insights.di

import data.storage.DailyInsightCache
import domain.analytics.usecase.GetLevelTransitionsUseCase
import domain.analytics.usecase.GetResponseTimeTrendUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import feature.insights.InsightsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun insightsModule() = module {
    factoryOf(::GetWordRushInsightsUseCase)

    viewModel {
        InsightsViewModel(
            getStudyInsightsUseCase = get(),
            getDifficultWordsUseCase = get(),
            getAccuracyTrendUseCase = get(),
            getAccuracyByLevelUseCase = get(),
            getStudyHeatmapUseCase = get(),
            getBestStudyTimeUseCase = get(),
            getWordRushInsightsUseCase = get(),
            getWeeklyReportUseCase = get(),
            getLevelTransitionsUseCase = get(),
            getResponseTimeTrendUseCase = get(),
            getProfileStatsUseCase = get(),
            dailyInsightCache = get<DailyInsightCache>(),
        )
    }
}
