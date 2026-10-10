package di

import data.analytics.local.IAnalyticsLocalQueue
import data.analytics.local.LexiconAnalyticsLocalQueue
import data.analytics.remote.AnalyticsStatsRemoteDataSource
import data.analytics.remote.AnalyticsWordRemoteDataSource
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.IAnalyticsWordDataSource
import data.analytics.repository.AnalyticsRecorderImpl
import data.analytics.repository.AnalyticsStatsRepositoryImpl
import data.analytics.repository.AnalyticsWordRepositoryImpl
import data.insights.local.IInsightsScreenLocalDataSource
import data.insights.local.InsightsScreenLocalDataSource
import data.insights.remote.IInsightsScreenRemoteDataSource
import data.insights.remote.InsightsScreenRemoteDataSource
import data.insights.repository.DismissedCoachCardsRepositoryImpl
import data.insights.repository.InsightsScreenRepositoryImpl
import data.wordrush.remote.IWordRushDataSource
import data.wordrush.remote.WordRushRemoteDataSource
import data.wordrush.repository.WordRushRecorderImpl
import data.wordrush.repository.WordRushStatsRepositoryImpl
import domain.analytics.repository.IAnalyticsRecorder
import domain.analytics.repository.IAnalyticsStatsRepository
import domain.analytics.repository.IAnalyticsWordRepository
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.RetryAnalyticsSyncUseCase
import domain.analytics.usecase.GetAccuracyByLevelUseCase
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetBestStudyTimeUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import domain.analytics.usecase.GetStudyInsightsUseCase
import domain.analytics.usecase.GetLevelTransitionsUseCase
import domain.analytics.usecase.GetResponseTimeTrendUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import domain.insights.repository.IDismissedCoachCardsRepository
import domain.insights.repository.IInsightsScreenRepository
import domain.insights.usecase.DismissCoachCardUseCase
import domain.insights.usecase.ObserveDismissedCoachCardsUseCase
import domain.insights.usecase.ObserveInsightsScreenUseCase
import domain.insights.usecase.RefreshInsightsScreenUseCase
import domain.wordrush.repository.IWordRushRecorder
import domain.wordrush.repository.IWordRushStatsRepository
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.wordrush.usecase.RecordWordRushGameUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun analyticsModule() = module {

    // Remote Data Sources
    singleOf(::AnalyticsStatsRemoteDataSource) bind IAnalyticsStatsDataSource::class
    singleOf(::AnalyticsWordRemoteDataSource) bind IAnalyticsWordDataSource::class

    // Local Queue
    singleOf(::LexiconAnalyticsLocalQueue) bind IAnalyticsLocalQueue::class

    // Repositories
    singleOf(::AnalyticsRecorderImpl) bind IAnalyticsRecorder::class
    singleOf(::AnalyticsStatsRepositoryImpl) bind IAnalyticsStatsRepository::class
    singleOf(::AnalyticsWordRepositoryImpl) bind IAnalyticsWordRepository::class

    // Use Cases — recording
    singleOf(::StartStudySessionUseCase)
    singleOf(::EndStudySessionUseCase)
    singleOf(::RecordReviewEventUseCase)
    singleOf(::RetryAnalyticsSyncUseCase)

    // Use Cases — querying
    singleOf(::GetStudyInsightsUseCase)
    singleOf(::GetDifficultWordsUseCase)
    singleOf(::GetAccuracyTrendUseCase)
    singleOf(::GetAccuracyByLevelUseCase)
    singleOf(::GetStudyHeatmapUseCase)
    singleOf(::GetBestStudyTimeUseCase)
    singleOf(::GetWeeklyReportUseCase)
    singleOf(::GetLevelTransitionsUseCase)
    singleOf(::GetResponseTimeTrendUseCase)

    // --- Word Rush Analytics ---

    // Remote Data Source
    singleOf(::WordRushRemoteDataSource) bind IWordRushDataSource::class

    // Repositories
    singleOf(::WordRushRecorderImpl) bind IWordRushRecorder::class
    singleOf(::WordRushStatsRepositoryImpl) bind IWordRushStatsRepository::class

    // Use Cases
    factoryOf(::RecordWordRushGameUseCase)
    factoryOf(::GetWordRushInsightsUseCase)

    // --- Insights coach screen (cached server screen + dismissed cards) ---

    // Data Sources (explicit lambdas: constructors have defaulted params Koin must not resolve)
    single<IInsightsScreenRemoteDataSource> { InsightsScreenRemoteDataSource(apiClient = get()) }
    single<IInsightsScreenLocalDataSource> { InsightsScreenLocalDataSource(queries = get()) }

    // Repositories
    single<IInsightsScreenRepository> { InsightsScreenRepositoryImpl(remote = get(), local = get()) }
    single<IDismissedCoachCardsRepository> { DismissedCoachCardsRepositoryImpl(local = get()) }

    // Use Cases
    factoryOf(::ObserveInsightsScreenUseCase)
    factoryOf(::RefreshInsightsScreenUseCase)
    factoryOf(::ObserveDismissedCoachCardsUseCase)
    factoryOf(::DismissCoachCardUseCase)
}
