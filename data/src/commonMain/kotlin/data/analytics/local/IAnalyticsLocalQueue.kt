package data.analytics.local

interface IAnalyticsLocalQueue {
    suspend fun insertRequest(requestJson: String, createdAt: Long)
    suspend fun getAllRequests(): List<String>
    suspend fun clearQueue()
}
