package domain.featureflag

/**
 * KAN-20: Dynamic feature flag provider interface.
 * Supports boolean, string, number, and JSON flag types.
 * Implementations must be offline-safe with local defaults fallback.
 */
interface IFeatureFlagProvider {

    /** Fetch latest flag values from remote. Call on app start. */
    suspend fun fetchAndActivate()

    fun getBoolean(key: String, default: Boolean = false): Boolean

    fun getString(key: String, default: String = ""): String

    fun getLong(key: String, default: Long = 0L): Long

    fun getDouble(key: String, default: Double = 0.0): Double
}
