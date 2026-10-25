package di

import data.connectivity.ServerStatusRepositoryImpl
import data.connectivity.remote.IServerHealthRemoteDataSource
import data.connectivity.remote.ServerHealthRemoteDataSource
import domain.connectivity.repository.IServerStatusRepository
import domain.connectivity.usecase.CheckServerStatusUseCase
import domain.connectivity.usecase.ObserveServerStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Live server status. The device's network monitor comes from each platform module. */
fun connectivityModule() = module {
    single<IServerHealthRemoteDataSource> { ServerHealthRemoteDataSource(apiClient = get()) }
    single<IServerStatusRepository> {
        ServerStatusRepositoryImpl(
            remote = get(),
            network = get(),
            reachability = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }

    factoryOf(::ObserveServerStatusUseCase)
    factoryOf(::CheckServerStatusUseCase)
}
