package di

import data.auth.refresh.ITokenRefreshManager
import data.auth.refresh.TokenRefreshManager
import data.auth.remote.AuthDataSource
import data.auth.remote.FeatureAccessRemoteDataSource
import data.auth.remote.IAuthDataSource
import data.auth.remote.IFeatureAccessRemoteDataSource
import data.auth.repository.AuthRepositoryImpl
import data.auth.session.SessionManager
import domain.auth.session.ISessionManager
import data.auth.state.AuthenticationStateManager
import data.auth.state.IAuthenticationStateManager
import data.auth.token.ITokenManager
import data.auth.token.TokenManager
import data.session.repository.SessionRepositoryImpl
import data.storage.SecureStorageAdapter
import domain.auth.repository.IAuthRepository
import domain.auth.repository.ISessionRepository
import domain.auth.service.AuthenticationService
import domain.auth.service.IAuthenticationService
import domain.auth.storage.ISecureStorage
import domain.auth.usecase.ClearAllUserDataUseCase
import domain.auth.usecase.DeleteAccountUseCase
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.auth.usecase.IsAuthenticatedUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.LogoutUseCase
import domain.auth.usecase.VerifySessionUseCase
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun authModule(backendUrl: String) = module {

    // Secure Storage - Domain abstraction
    single<ISecureStorage> { SecureStorageAdapter(platformStorage = get()) }

    // Auth Components
    single<ITokenManager> { TokenManager(secureStorage = get()) }
    single<IAuthenticationStateManager> { AuthenticationStateManager(tokenManager = get()) }
    single<ISessionManager> { SessionManager(authenticationStateManager = get()) }

    // Token Refresh Manager
    single<ITokenRefreshManager> {
        TokenRefreshManager(
            tokenManager = get(),
            authDataSource = get<IAuthDataSource>(),
            authenticationStateManager = get()
        )
    }

    // Data Sources
    single<IAuthDataSource> { AuthDataSource(backendUrl, get<HttpClient>()) }
    single<IFeatureAccessRemoteDataSource> { FeatureAccessRemoteDataSource(apiClient = get(), featureFlagProvider = get()) }

    // Repositories
    single<IAuthRepository> {
        AuthRepositoryImpl(
            tokenManager = get(),
            sessionManager = get(),
            featureAccessRemoteDataSource = get(),
            authDataSource = get(),
            googleAuthStateProvider = get(),
            appleAuthStateProvider = get()
        )
    }

    single<ISessionRepository> {
        SessionRepositoryImpl(
            authDataSource = get(),
            secureStorage = get()
        )
    }

    // Domain Services
    single<IAuthenticationService> { AuthenticationService(authRepository = get<IAuthRepository>()) }

    // Use Cases - Feature Access
    singleOf(::GetFeatureAccessUseCase)

    // Use Cases - Authentication
    singleOf(::LoginWithGoogleUseCase)
    singleOf(::LoginWithAppleUseCase)
    singleOf(::LogoutUseCase)
    singleOf(::DeleteAccountUseCase)
    singleOf(::IsAuthenticatedUseCase)
    singleOf(::VerifySessionUseCase)
    singleOf(::ClearAllUserDataUseCase)
}
