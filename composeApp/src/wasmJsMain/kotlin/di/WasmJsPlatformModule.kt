package di

import app.cash.sqldelight.db.SqlDriver
import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import auth.WasmJsAppleAuthStateProvider
import auth.WasmJsGoogleAuthStateProvider
import data.core.database.DatabaseDriverFactory
import data.core.database.LexiconDatabase
import data.storage.DailyInsightCache
import data.storage.SecureStorage
import data.storage.WasmJsDailyInsightCache
import data.storage.WasmJsSecureStorage
import data.subscription.WebSubscriptionManager
import domain.connectivity.INetworkMonitor
import domain.subscription.ISubscriptionManager
import domain.widget.IWidgetRefresher
import org.koin.dsl.module
import platform.IAppVersionProvider
import platform.WasmJsAppVersionProvider
import platform.WasmJsNetworkMonitor

fun wasmJsPlatformModule() = module {
    // Database
    single<SqlDriver> { DatabaseDriverFactory().createDriver() }
    single { LexiconDatabase(get<SqlDriver>()) }
    single { get<LexiconDatabase>().lexiconQueries }

    // Secure Storage
    single<SecureStorage> { WasmJsSecureStorage() }

    // Daily Insight Cache
    single<DailyInsightCache> { WasmJsDailyInsightCache() }

    // App Version Provider
    single<IAppVersionProvider> { WasmJsAppVersionProvider() }

    // Auth State Providers
    single<IAppleAuthStateProvider> { WasmJsAppleAuthStateProvider() }
    single<IGoogleAuthStateProvider> { WasmJsGoogleAuthStateProvider() }

    // Widget Refresher (no-op on web)
    single<IWidgetRefresher> { widget.NoOpWidgetRefresher }

    // Device network state, for the server status
    single<INetworkMonitor> { WasmJsNetworkMonitor() }

    // Subscription Manager (web stub)
    single<ISubscriptionManager> { WebSubscriptionManager() }
}
