package di

import app.cash.sqldelight.db.SqlDriver
import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import auth.WasmJsAppleAuthStateProvider
import auth.WasmJsGoogleAuthStateProvider
import data.core.database.DatabaseDriverFactory
import data.core.database.LexiconDatabase
import data.storage.SecureStorage
import data.storage.WasmJsSecureStorage
import data.subscription.WebSubscriptionManager
import domain.subscription.ISubscriptionManager
import org.koin.dsl.module
import platform.IAppVersionProvider
import platform.WasmJsAppVersionProvider

fun wasmJsPlatformModule() = module {
    // Database
    single<SqlDriver> { DatabaseDriverFactory().createDriver() }
    single { LexiconDatabase(get<SqlDriver>()) }
    single { get<LexiconDatabase>().lexiconQueries }

    // Secure Storage
    single<SecureStorage> { WasmJsSecureStorage() }

    // App Version Provider
    single<IAppVersionProvider> { WasmJsAppVersionProvider() }

    // Auth State Providers
    single<IAppleAuthStateProvider> { WasmJsAppleAuthStateProvider() }
    single<IGoogleAuthStateProvider> { WasmJsGoogleAuthStateProvider() }

    // Subscription Manager (web stub)
    single<ISubscriptionManager> { WebSubscriptionManager() }
}
