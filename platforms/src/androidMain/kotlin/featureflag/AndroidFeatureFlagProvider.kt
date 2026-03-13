package featureflag

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import domain.featureflag.IFeatureFlagProvider
import kotlinx.coroutines.tasks.await

/**
 * KAN-20: Android implementation using Firebase Remote Config.
 * - Fetch interval: 0s for debug, 12h for release
 * - Offline-safe with local defaults fallback
 */
class AndroidFeatureFlagProvider : IFeatureFlagProvider {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (isDebugBuild()) 0L else 43200L // 12h
        }
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(defaults)
    }

    override suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (_: Exception) {
            // Offline-safe: silently fall back to cached/default values
        }
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (remoteConfig.all.containsKey(key)) remoteConfig.getBoolean(key) else default

    override fun getString(key: String, default: String): String {
        val value = remoteConfig.getString(key)
        return value.ifEmpty { default }
    }

    override fun getLong(key: String, default: Long): Long =
        if (remoteConfig.all.containsKey(key)) remoteConfig.getLong(key) else default

    override fun getDouble(key: String, default: Double): Double =
        if (remoteConfig.all.containsKey(key)) remoteConfig.getDouble(key) else default

    companion object {
        /** Local defaults — used when Remote Config has no fetched values. */
        private val defaults = mapOf<String, Any>(
            "push_notifications_enabled" to true,
            "ai_import_enabled" to true,
            "leaderboard_enabled" to true,
            "max_free_words" to 50L,
        )

        private fun isDebugBuild(): Boolean = try {
            Class.forName("com.alirezaiyan.vokab.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        } catch (_: Exception) {
            false
        }
    }
}

actual fun createFeatureFlagProvider(): IFeatureFlagProvider = AndroidFeatureFlagProvider()
