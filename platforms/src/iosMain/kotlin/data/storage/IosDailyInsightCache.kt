package data.storage

import platform.Foundation.NSUserDefaults

class IosDailyInsightCache : DailyInsightCache {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getDailyInsight(): String? =
        defaults.stringForKey(KEY_DAILY_INSIGHT)

    override fun saveDailyInsight(message: String) {
        defaults.setObject(message, KEY_DAILY_INSIGHT)
    }

    override fun clearDailyInsight() {
        defaults.removeObjectForKey(KEY_DAILY_INSIGHT)
    }

    companion object {
        private const val KEY_DAILY_INSIGHT = "lexicon_daily_insight"
    }
}
