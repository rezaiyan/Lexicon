package featureflag

import domain.featureflag.IFeatureFlagProvider
import platform.Foundation.NSLog

/**
 * KAN-20: iOS stub — returns defaults.
 * Firebase Remote Config for iOS is handled by the Swift layer.
 */
class IOSFeatureFlagProvider : IFeatureFlagProvider {

    private val defaults = mutableMapOf<String, Any>(
        "push_notifications_enabled" to true,
        "ai_import_enabled" to true,
        "leaderboard_enabled" to true,
        "max_free_words" to 50L,
    )

    override suspend fun fetchAndActivate() {
        NSLog("[FeatureFlags] Fetch requested (iOS stub — using defaults)")
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        defaults[key] as? Boolean ?: default

    override fun getString(key: String, default: String): String =
        defaults[key] as? String ?: default

    override fun getLong(key: String, default: Long): Long =
        defaults[key] as? Long ?: default

    override fun getDouble(key: String, default: Double): Double =
        defaults[key] as? Double ?: default
}

actual fun createFeatureFlagProvider(): IFeatureFlagProvider = IOSFeatureFlagProvider()
