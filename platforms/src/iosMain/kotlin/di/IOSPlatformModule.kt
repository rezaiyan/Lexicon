package di

import auth.IAppleAuthStateProvider
import auth.IOSAppleAuthStateProvider
import data.storage.DailyInsightCache
import data.storage.IOSKeychainSecureStorage
import data.storage.IOSPlatformSecureStorage
import data.storage.IosDailyInsightCache
import data.storage.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.IAppVersionProvider
import platform.IOSAppVersionProvider

/**
 * iOS platform-specific module
 */
fun iosPlatformModule(): Module = module {
    // Note: AppDatabase and DatabaseDriverFactory are provided by data module
    // They are defined in data/src/*Main/kotlin/data/core/database/

    // Secure Storage
    single<IOSPlatformSecureStorage> { IOSKeychainSecureStorage() }
    single<SecureStorage> { get<IOSPlatformSecureStorage>() }

    // Daily Insight Cache
    single<DailyInsightCache> { IosDailyInsightCache() }
    
    // App Version Provider
    single<IAppVersionProvider> {
        IOSAppVersionProvider()
    }

    // Apple Auth State Provider
    single<IAppleAuthStateProvider> {
        IOSAppleAuthStateProvider()
    }
    
    // Note: IOSAccountDeletionHandler is defined in composeApp module, not platforms
    
}


