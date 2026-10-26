package di

import data.passkey.PassKiAuthenticator
import data.passkey.PasskeyRepositoryImpl
import data.passkey.PasskeySignalRepositoryImpl
import data.passkey.remote.IPasskeyRemoteDataSource
import data.passkey.remote.PasskeyRemoteDataSource
import domain.passkey.IPasskeyAuthenticator
import domain.passkey.repository.IPasskeyRepository
import domain.passkey.repository.IPasskeySignalRepository
import domain.passkey.usecase.CreatePasskeyUseCase
import domain.passkey.usecase.DeletePasskeyUseCase
import domain.passkey.usecase.GetPasskeysUseCase
import domain.passkey.usecase.IsPasskeyAvailableUseCase
import domain.passkey.usecase.SignInWithPasskeyUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Passkeys: an extra sign-in for an existing account (created in Settings, used on the sign-in screen). */
fun passkeyModule() = module {
    single<IPasskeyAuthenticator> { PassKiAuthenticator() }
    single<IPasskeyRemoteDataSource> { PasskeyRemoteDataSource(apiClient = get()) }
    single<IPasskeyRepository> { PasskeyRepositoryImpl(remote = get()) }
    single<IPasskeySignalRepository> { PasskeySignalRepositoryImpl(queries = get()) }

    factoryOf(::IsPasskeyAvailableUseCase)
    factoryOf(::CreatePasskeyUseCase)
    factoryOf(::GetPasskeysUseCase)
    factoryOf(::DeletePasskeyUseCase)
    factoryOf(::SignInWithPasskeyUseCase)
}
