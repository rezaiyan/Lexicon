package feature.insights

import domain.analytics.usecase.GetAccuracyByLevelUseCase
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetBestStudyTimeUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetLevelTransitionsUseCase
import domain.analytics.usecase.GetResponseTimeTrendUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import domain.analytics.usecase.GetStudyInsightsUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.profile.usecase.GetProfileStatsUseCase
import domain.wordrush.usecase.GetWordRushInsightsUseCase

/** Bundles the analytics use cases [InsightsDataLoader] fetches on refresh. */
data class InsightsUseCases(
    val getStudyInsights: GetStudyInsightsUseCase,
    val getDifficultWords: GetDifficultWordsUseCase,
    val getAccuracyTrend: GetAccuracyTrendUseCase,
    val getAccuracyByLevel: GetAccuracyByLevelUseCase,
    val getStudyHeatmap: GetStudyHeatmapUseCase,
    val getBestStudyTime: GetBestStudyTimeUseCase,
    val getWordRushInsights: GetWordRushInsightsUseCase,
    val getWeeklyReport: GetWeeklyReportUseCase,
    val getLevelTransitions: GetLevelTransitionsUseCase,
    val getResponseTimeTrend: GetResponseTimeTrendUseCase,
    val getProfileStats: GetProfileStatsUseCase,
)
