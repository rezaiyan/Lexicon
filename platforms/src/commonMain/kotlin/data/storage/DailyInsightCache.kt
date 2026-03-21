package data.storage

interface DailyInsightCache {
    fun getDailyInsight(): String?
    fun saveDailyInsight(message: String)
    fun clearDailyInsight()
}
