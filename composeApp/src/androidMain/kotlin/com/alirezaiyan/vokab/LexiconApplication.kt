package com.alirezaiyan.vokab

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.alirezaiyan.vokab.widget.DailyWordWidgetWorker
import config.AppConfig
import di.androidPlatformModule
import di.appModule
import di.mobileModule
import domain.featureflag.IFeatureFlagProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin

/**
 * Application class for Lexicon
 * Handles:
 * - Firebase initialization (Analytics, Crashlytics)
 * - Koin dependency injection setup
 * - Notification channel creation
 * - Google Auth initialization
 * - Coil image loading configuration
 */
class LexiconApplication : Application(), SingletonImageLoader.Factory,
    androidx.work.Configuration.Provider {

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase Analytics
        analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(true)
        Log.d(TAG, "Firebase Analytics initialized")
        
        // Initialize Firebase Crashlytics
        crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = true
        Log.d(TAG, "Firebase Crashlytics initialized")
        
        // Initialize KMPAuth GoogleAuthProvider
        GoogleAuthProvider.create(
            credentials = GoogleAuthCredentials(
                serverId = AppConfig.GOOGLE_SERVER_CLIENT_ID
            )
        )
        Log.d(TAG, "GoogleAuthProvider initialized")
        
        // Initialize RevenueCat
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        Purchases.configure(
            PurchasesConfiguration(AppConfig.REVENUECAT_ANDROID_KEY)
        )
        Log.d(TAG, "RevenueCat initialized")
        
        // Create notification channel
        createNotificationChannel()
        
        // Initialize Koin
        startKoin {
            androidContext(this@LexiconApplication)
            modules(
                androidPlatformModule(this@LexiconApplication),
                mobileModule(),
                appModule(
                    backendUrl = AppConfig.VOKAB_BACKEND_URL,
                    platform = data.notification.remote.model.Platform.ANDROID
                )
            )
        }
        
        // Fetch remote feature flags
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val featureFlagProvider = getKoin().get<IFeatureFlagProvider>()
                featureFlagProvider.fetchAndActivate()
                Log.d(TAG, "Feature flags fetched and activated")
            } catch (e: Exception) {
                Log.w(TAG, "Feature flags fetch failed — using defaults", e)
            }
        }

        // Schedule daily widget updates
        DailyWordWidgetWorker.enqueue(this)

        // Log app start event
        analytics.logEvent("app_start", null)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "lexicon_notifications",
                "Lexicon Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vocabulary learning reminders, streak alerts, and achievements"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    // Coil ImageLoader factory
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
    
    companion object {
        private const val TAG = "LexiconApp"
    }
}

