package data.word.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteWord(
    val id: Long? = null,
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: Int,
    val easeFactor: Float,
    val interval: Int,
    val repetitions: Int,
    val lastReviewDate: Long,
    val nextReviewDate: Long,
    val createdAt: Long? = null
)

@Serializable
data class UpsertWordsPayload(
    val words: List<RemoteWord>
)

