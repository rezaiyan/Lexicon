package featureflag

import domain.featureflag.IFeatureFlagProvider

/**
 * KAN-20: WasmJs stub — returns defaults.
 */
class WasmJsFeatureFlagProvider : IFeatureFlagProvider {

    private val defaults = mutableMapOf<String, Any>(
        "push_notifications_enabled" to true,
        "ai_import_enabled" to true,
        "leaderboard_enabled" to true,
        "max_free_words" to 50L,
    )

    override suspend fun fetchAndActivate() {
        println("[FeatureFlags] Fetch requested (WasmJs stub — using defaults)")
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

actual fun createFeatureFlagProvider(): IFeatureFlagProvider = WasmJsFeatureFlagProvider()
