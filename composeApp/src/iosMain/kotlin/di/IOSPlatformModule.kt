package di

import account.IOSAccountDeletionHandler
import data.core.database.DatabaseDriverFactory
import data.core.database.LexiconDatabase
import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import auth.IOSAppleAuthStateProvider
import auth.IOSGoogleAuthStateProvider
import data.storage.IOSKeychainSecureStorage
import data.storage.IOSPlatformSecureStorage
import data.storage.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import domain.widget.IWidgetRefresher
import platform.IAppVersionProvider
import platform.IOSAppVersionProvider
import widget.IosWidgetRefresher

/**
 * iOS platform-specific module
 */
fun iosPlatformModule(): Module = module {
    // Database
    val driver = DatabaseDriverFactory().createDriver()
    single { LexiconDatabase(driver) }
    single { get<LexiconDatabase>().lexiconQueries }

    // Secure Storage
    single<IOSPlatformSecureStorage> { IOSKeychainSecureStorage() }
    single<SecureStorage> { get<IOSPlatformSecureStorage>() }

    // App Version Provider
    single<IAppVersionProvider> {
        IOSAppVersionProvider()
    }

    // Apple Auth State Provider
    single<IAppleAuthStateProvider> {
        IOSAppleAuthStateProvider()
    }

    // Google Auth State Provider
    single<IGoogleAuthStateProvider> {
        IOSGoogleAuthStateProvider()
    }

    // Widget Refresher
    single<IWidgetRefresher> { IosWidgetRefresher() }

    // Account Deletion Handler
    single {
        IOSAccountDeletionHandler(
            queries = get(),
            secureStorage = get()
        )
    }
}
