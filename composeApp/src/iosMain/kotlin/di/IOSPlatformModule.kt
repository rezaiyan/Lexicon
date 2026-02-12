package di

import account.IOSAccountDeletionHandler
import data.core.database.AppDatabase
import data.core.database.DatabaseDriverFactory
import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import auth.IOSAppleAuthStateProvider
import auth.IOSGoogleAuthStateProvider
import data.storage.IOSKeychainSecureStorage
import data.storage.IOSPlatformSecureStorage
import data.storage.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.IAppVersionProvider
import platform.IOSAppVersionProvider

/**
 * iOS platform-specific module
 */
fun iosPlatformModule(): Module = module {
    // Database
    single<AppDatabase> { 
        DatabaseDriverFactory().createDatabase() 
    }
    
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
    
    // Account Deletion Handler
    single {
        IOSAccountDeletionHandler(
            dao = get<AppDatabase>().getDao(),
            secureStorage = get()
        )
    }
    
}


