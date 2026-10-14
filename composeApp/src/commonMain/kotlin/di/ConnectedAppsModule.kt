package di

import data.connectedapps.ConnectedAppsRepositoryImpl
import data.connectedapps.remote.ConnectedAppsRemoteDataSource
import data.connectedapps.remote.IConnectedAppsRemoteDataSource
import domain.connectedapps.repository.IConnectedAppsRepository
import domain.connectedapps.usecase.DisconnectAppUseCase
import domain.connectedapps.usecase.GetConnectedAppsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** AI apps connected over MCP (server-held grants, listed and revoked from Settings). */
fun connectedAppsModule() = module {
    single<IConnectedAppsRemoteDataSource> { ConnectedAppsRemoteDataSource(apiClient = get()) }
    single<IConnectedAppsRepository> { ConnectedAppsRepositoryImpl(remote = get()) }

    factoryOf(::GetConnectedAppsUseCase)
    factoryOf(::DisconnectAppUseCase)
}
