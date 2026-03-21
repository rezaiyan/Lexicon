package notification.payload

import data.storage.DailyInsightCache

class DailyInsightHandler(
    private val dailyInsightCache: DailyInsightCache,
) : NotificationPayloadHandler {

    override val type: String = "daily_insight"

    override suspend fun handle(data: Map<String, String>) {
        val message = data["body"] ?: return
        if (message.isNotBlank()) {
            dailyInsightCache.saveDailyInsight(message)
        }
    }
}
