package di

import data.support.SupportRepositoryImpl
import data.support.local.ISupportLocalDataSource
import data.support.local.ISupportReadStateLocalDataSource
import data.support.local.SupportLocalDataSource
import data.support.local.SupportReadStateLocalDataSource
import data.support.remote.ISupportRemoteDataSource
import data.support.remote.SupportRemoteDataSource
import domain.support.repository.ISupportRepository
import domain.support.usecase.MarkSupportReadUseCase
import domain.support.usecase.MarkSupportSeenInSettingsUseCase
import domain.support.usecase.ObserveSupportMessagesUseCase
import domain.support.usecase.ObserveSupportUnreadUseCase
import domain.support.usecase.SyncSupportRepliesUseCase
import domain.support.usecase.RefreshSupportMessagesUseCase
import domain.support.usecase.RetrySupportMessageUseCase
import domain.support.usecase.SendSupportMessageUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Support chat: local-first conversation synced with the backend. */
fun supportModule() = module {
    single<ISupportLocalDataSource> { SupportLocalDataSource(queries = get()) }
    single<ISupportRemoteDataSource> { SupportRemoteDataSource(apiClient = get()) }
    single<ISupportReadStateLocalDataSource> { SupportReadStateLocalDataSource(queries = get()) }
    single<ISupportRepository> { SupportRepositoryImpl(local = get(), remote = get(), readState = get()) }

    factoryOf(::ObserveSupportMessagesUseCase)
    factoryOf(::RefreshSupportMessagesUseCase)
    factoryOf(::SendSupportMessageUseCase)
    factoryOf(::RetrySupportMessageUseCase)
    factoryOf(::ObserveSupportUnreadUseCase)
    factoryOf(::MarkSupportReadUseCase)
    factoryOf(::MarkSupportSeenInSettingsUseCase)
    factoryOf(::SyncSupportRepliesUseCase)
}
