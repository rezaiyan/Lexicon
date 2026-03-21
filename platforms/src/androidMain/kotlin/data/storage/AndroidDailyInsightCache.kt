package data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AndroidDailyInsightCache(context: Context) : DailyInsightCache {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getDailyInsight(): String? =
        prefs.getString(KEY_DAILY_INSIGHT, null)

    override fun saveDailyInsight(message: String) {
        prefs.edit { putString(KEY_DAILY_INSIGHT, message) }
    }

    override fun clearDailyInsight() {
        prefs.edit { remove(KEY_DAILY_INSIGHT) }
    }

    companion object {
        private const val PREFS_NAME = "lexicon_insights_cache"
        private const val KEY_DAILY_INSIGHT = "daily_insight"
    }
}
