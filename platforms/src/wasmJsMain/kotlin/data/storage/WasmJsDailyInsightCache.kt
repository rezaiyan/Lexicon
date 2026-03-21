package data.storage

class WasmJsDailyInsightCache : DailyInsightCache {

    private var cached: String? = null

    override fun getDailyInsight(): String? = cached

    override fun saveDailyInsight(message: String) {
        cached = message
    }

    override fun clearDailyInsight() {
        cached = null
    }
}
