package data.listening.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncListeningRequest(
    val sessions: List<SyncListeningSessionRequest>,
)

@Serializable
data class SyncListeningSessionRequest(
    val clientSessionId: String,
    val source: String,
    val sourceDetail: String? = null,
    val order: String,
    val repeatCount: Int,
    val pauseMs: Long,
    val speechRate: Float,
    val plannedWords: Int,
    val wordsHeard: Int,
    val wordsSkipped: Int,
    val pauseCount: Int,
    val listeningMs: Long,
    val durationMs: Long,
    val completedNormally: Boolean,
    val startedAt: Long,
    val endedAt: Long,
    val words: List<SyncListeningWordRequest>,
)

@Serializable
data class SyncListeningWordRequest(
    val wordId: Long,
    val sourceLanguage: String,
    val targetLanguage: String,
    val heardAt: Long,
)
