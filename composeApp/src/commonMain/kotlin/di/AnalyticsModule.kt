package di

import data.analytics.remote.AnalyticsRemoteDataSource
import data.analytics.remote.IAnalyticsRemoteDataSource
import data.analytics.repository.AnalyticsRecorderImpl
import data.analytics.repository.AnalyticsRepositoryImpl
import domain.analytics.repository.IAnalyticsRecorder
import domain.analytics.repository.IAnalyticsRepository
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.GetAccuracyByLevelUseCase
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetBestStudyTimeUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import domain.analytics.usecase.GetStudyInsightsUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun analyticsModule() = module {

    // Remote Data Source
    single<IAnalyticsRemoteDataSource> { AnalyticsRemoteDataSource(apiClient = get()) }

    // Repositories
    single { AnalyticsRecorderImpl(remoteDataSource = get()) } bind IAnalyticsRecorder::class
    single { AnalyticsRepositoryImpl(remoteDataSource = get()) } bind IAnalyticsRepository::class

    // Use Cases — recording
    singleOf(::StartStudySessionUseCase)
    singleOf(::EndStudySessionUseCase)
    singleOf(::RecordReviewEventUseCase)

    // Use Cases — querying
    singleOf(::GetStudyInsightsUseCase)
    singleOf(::GetDifficultWordsUseCase)
    singleOf(::GetAccuracyTrendUseCase)
    singleOf(::GetAccuracyByLevelUseCase)
    singleOf(::GetStudyHeatmapUseCase)
    singleOf(::GetBestStudyTimeUseCase)
    singleOf(::GetWeeklyReportUseCase)
}
