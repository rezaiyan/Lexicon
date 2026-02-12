package data.storage

/**
 * WasmJs implementation of SecureStorage
 * Uses browser localStorage with "lexicon_" prefix
 *
 * Note: localStorage is NOT truly secure for sensitive data like tokens.
 * This is acceptable for a web demo/MVP but should be reviewed for production.
 */
class WasmJsSecureStorage : SecureStorage {

    private companion object {
        const val PREFIX = "lexicon_"
        const val KEY_ACCESS_TOKEN = "lexicon_access_token"
        const val KEY_REFRESH_TOKEN = "lexicon_refresh_token"
        const val KEY_INSIGHT_ID = "lexicon_insight_id"
        const val KEY_INSIGHT_DATE = "lexicon_insight_date"
        const val KEY_INSIGHT_TIMESTAMP = "lexicon_insight_timestamp"
        const val KEY_ONBOARDING_COMPLETED = "lexicon_onboarding_completed"
    }

    override suspend fun saveAccessToken(token: String) {
        setItem(KEY_ACCESS_TOKEN, token)
    }

    override suspend fun saveRefreshToken(token: String) {
        setItem(KEY_REFRESH_TOKEN, token)
    }

    override fun getAccessToken(): String? {
        return getItem(KEY_ACCESS_TOKEN)
    }

    override suspend fun getRefreshToken(): String? {
        return getItem(KEY_REFRESH_TOKEN)
    }

    override suspend fun clearTokens() {
        removeItem(KEY_ACCESS_TOKEN)
        removeItem(KEY_REFRESH_TOKEN)
    }

    override suspend fun storeDailyInsightData(insightId: String, date: String, timestamp: Long) {
        setItem(KEY_INSIGHT_ID, insightId)
        setItem(KEY_INSIGHT_DATE, date)
        setItem(KEY_INSIGHT_TIMESTAMP, timestamp.toString())
    }

    override suspend fun getDailyInsightData(): DailyInsightData? {
        val insightId = getItem(KEY_INSIGHT_ID) ?: return null
        val date = getItem(KEY_INSIGHT_DATE) ?: return null
        val timestamp = getItem(KEY_INSIGHT_TIMESTAMP)?.toLongOrNull() ?: return null
        return DailyInsightData(insightId, date, timestamp)
    }

    override suspend fun clearDailyInsightData() {
        removeItem(KEY_INSIGHT_ID)
        removeItem(KEY_INSIGHT_DATE)
        removeItem(KEY_INSIGHT_TIMESTAMP)
    }

    override suspend fun hasCompletedOnboarding(): Boolean {
        return getItem(KEY_ONBOARDING_COMPLETED) == "true"
    }

    override suspend fun markOnboardingCompleted() {
        setItem(KEY_ONBOARDING_COMPLETED, "true")
    }

    private fun setItem(key: String, value: String) {
        jsSetItem(key, value)
    }

    private fun getItem(key: String): String? {
        val result = jsGetItem(key)
        return result?.toString()
    }

    private fun removeItem(key: String) {
        jsRemoveItem(key)
    }
}

private fun jsSetItem(key: String, value: String): JsAny? =
    js("localStorage.setItem(key, value)")

private fun jsGetItem(key: String): JsAny? =
    js("localStorage.getItem(key)")

private fun jsRemoveItem(key: String): JsAny? =
    js("localStorage.removeItem(key)")
