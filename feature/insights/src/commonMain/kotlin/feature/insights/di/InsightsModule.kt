package feature.insights.di

import feature.insights.InsightsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun insightsModule() = module {
    viewModel {
        InsightsViewModel(
            getStudyInsightsUseCase = get(),
            getDifficultWordsUseCase = get(),
            getAccuracyTrendUseCase = get(),
            getAccuracyByLevelUseCase = get(),
            getStudyHeatmapUseCase = get(),
            getBestStudyTimeUseCase = get(),
        )
    }
}
