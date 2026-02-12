package notification.payload

import data.storage.SecureStorage
import kotlin.time.Clock

class DailyInsightHandler(
    private val secureStorage: SecureStorage
) : NotificationPayloadHandler {
    
    override val type: String = "daily_insight"
    
    override suspend fun handle(data: Map<String, String>) {
        val insightId = data["insight_id"] ?: ""
        val date = data["date"] ?: ""
        
        secureStorage.storeDailyInsightData(
            insightId = insightId,
            date = date,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }
}

