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
        const val KEY_TOKEN_EXPIRES_AT = "lexicon_token_expires_at"
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
        removeItem(KEY_TOKEN_EXPIRES_AT)
    }

    override suspend fun saveTokenExpiresAt(expiresAtMs: Long) {
        setItem(KEY_TOKEN_EXPIRES_AT, expiresAtMs.toString())
    }

    override fun getTokenExpiresAt(): Long {
        return getItem(KEY_TOKEN_EXPIRES_AT)?.toLongOrNull() ?: 0L
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
