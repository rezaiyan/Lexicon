package di

import data.credits.remote.CreditsRemoteDataSource
import data.credits.remote.ICreditsRemoteDataSource
import data.credits.repository.CreditsRepositoryImpl
import domain.credits.ICreditsRepository
import domain.credits.usecase.ObserveCreditsUseCase
import domain.credits.usecase.RefreshCreditsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** AI credits: one process-wide balance cache shared by every screen that spends or shows credits. */
fun creditsModule() = module {
    singleOf(::CreditsRemoteDataSource) bind ICreditsRemoteDataSource::class
    singleOf(::CreditsRepositoryImpl) bind ICreditsRepository::class
    factoryOf(::ObserveCreditsUseCase)
    factoryOf(::RefreshCreditsUseCase)
}
