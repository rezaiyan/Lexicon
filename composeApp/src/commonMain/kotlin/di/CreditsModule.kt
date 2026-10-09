package di

import data.credits.remote.CreditsRemoteDataSource
import data.credits.remote.ICreditsRemoteDataSource
import data.credits.repository.CreditsRepositoryImpl
import domain.credits.ICreditsRepository
import domain.credits.usecase.ObserveCreditsUseCase
import domain.credits.usecase.RefreshCreditsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** AI credits: one process-wide balance cache shared by every screen that spends or shows credits. */
fun creditsModule() = module {
    singleOf(::CreditsRemoteDataSource) bind ICreditsRemoteDataSource::class
    single<ICreditsRepository> {
        CreditsRepositoryImpl(
            remote = get(),
            userManager = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    factoryOf(::ObserveCreditsUseCase)
    factoryOf(::RefreshCreditsUseCase)
}
