package di

import android.content.Context
import auth.AndroidAppleAuthStateProvider
import auth.AndroidGoogleAuthStateProvider
import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import data.core.database.AppDatabase
import data.core.database.DatabaseDriverFactory
import data.storage.AndroidSecureStorage
import data.storage.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.AndroidAppVersionProvider
import platform.IAppVersionProvider

/**
 * Android platform-specific module
 * Provides Activity-independent implementations
 */
fun androidPlatformModule(context: Context): Module = module {
    // Database
    single<AppDatabase> { 
        DatabaseDriverFactory(context).createDatabase() 
    }
    
    // Application Context
    single<Context> { context }
    
    // Secure Storage
    single<SecureStorage> { 
        AndroidSecureStorage(context) 
    }
    
    // App Version Provider
    single<IAppVersionProvider> {
        AndroidAppVersionProvider(get())
    }

    // Apple Auth State Provider
    single<IAppleAuthStateProvider> {
        AndroidAppleAuthStateProvider()
    }
    
    single<IGoogleAuthStateProvider> {
        AndroidGoogleAuthStateProvider()
    }
    
    // Notification Display Service
    single<notification.NotificationDisplayService> {
        notification.AndroidNotificationDisplayService(
            context = get()
        )
    }
}

