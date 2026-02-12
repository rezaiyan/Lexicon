package data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android implementation using EncryptedSharedPreferences
 */
class AndroidSecureStorage(context: Context) : SecureStorage {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "vokab_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override suspend fun saveAccessToken(token: String) {
        sharedPreferences.edit { putString(KEY_ACCESS_TOKEN, token) }
    }
    
    override suspend fun saveRefreshToken(token: String) {
        sharedPreferences.edit { putString(KEY_REFRESH_TOKEN, token) }
    }
    
    override fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    override suspend fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    override suspend fun clearTokens() {
        sharedPreferences.edit {
            remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_DAILY_INSIGHT_ID)
                .remove(KEY_DAILY_INSIGHT_DATE)
                .remove(KEY_DAILY_INSIGHT_TIMESTAMP)
        }
    }
    
    override suspend fun storeDailyInsightData(insightId: String, date: String, timestamp: Long) {
        sharedPreferences.edit {
            putString(KEY_DAILY_INSIGHT_ID, insightId)
            putString(KEY_DAILY_INSIGHT_DATE, date)
            putLong(KEY_DAILY_INSIGHT_TIMESTAMP, timestamp)
        }
    }
    
    override suspend fun getDailyInsightData(): DailyInsightData? {
        val insightId = sharedPreferences.getString(KEY_DAILY_INSIGHT_ID, null)
        val date = sharedPreferences.getString(KEY_DAILY_INSIGHT_DATE, null)
        val timestamp = sharedPreferences.getLong(KEY_DAILY_INSIGHT_TIMESTAMP, 0L)
        
        return if (insightId != null && date != null && timestamp > 0) {
            DailyInsightData(insightId, date, timestamp)
        } else {
            null
        }
    }
    
    override suspend fun clearDailyInsightData() {
        sharedPreferences.edit {
            remove(KEY_DAILY_INSIGHT_ID)
            remove(KEY_DAILY_INSIGHT_DATE)
            remove(KEY_DAILY_INSIGHT_TIMESTAMP)
        }
    }

    override suspend fun hasCompletedOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override suspend fun markOnboardingCompleted() {
        sharedPreferences.edit { putBoolean(KEY_ONBOARDING_COMPLETED, true) }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_DAILY_INSIGHT_ID = "daily_insight_id"
        private const val KEY_DAILY_INSIGHT_DATE = "daily_insight_date"
        private const val KEY_DAILY_INSIGHT_TIMESTAMP = "daily_insight_timestamp"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}


