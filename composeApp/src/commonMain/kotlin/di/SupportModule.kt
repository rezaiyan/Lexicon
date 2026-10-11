package di

import data.support.SupportRepositoryImpl
import data.support.local.ISupportLocalDataSource
import data.support.local.SupportLocalDataSource
import data.support.remote.ISupportRemoteDataSource
import data.support.remote.SupportRemoteDataSource
import domain.support.repository.ISupportRepository
import domain.support.usecase.ObserveSupportMessagesUseCase
import domain.support.usecase.RefreshSupportMessagesUseCase
import domain.support.usecase.RetrySupportMessageUseCase
import domain.support.usecase.SendSupportMessageUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Support chat: local-first conversation synced with the backend. */
fun supportModule() = module {
    single<ISupportLocalDataSource> { SupportLocalDataSource(queries = get()) }
    single<ISupportRemoteDataSource> { SupportRemoteDataSource(apiClient = get()) }
    single<ISupportRepository> { SupportRepositoryImpl(local = get(), remote = get()) }

    factoryOf(::ObserveSupportMessagesUseCase)
    factoryOf(::RefreshSupportMessagesUseCase)
    factoryOf(::SendSupportMessageUseCase)
    factoryOf(::RetrySupportMessageUseCase)
}
