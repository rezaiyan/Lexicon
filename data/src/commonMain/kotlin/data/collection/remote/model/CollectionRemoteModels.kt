package data.collection.remote.model

import kotlinx.serialization.Serializable

/**
 * Models for vocabulary collection API
 * Structure: TargetLanguage/OriginLanguage/fileName.txt
 */

@Serializable
data class VocabularyCollection(
    val targetLanguage: String,    // What user wants to learn
    val originLanguage: String,     // User's native language
    val title: String,
    val fileName: String,
    val path: String
)

@Serializable
data class DownloadCollectionRequest(
    val targetLanguage: String,
    val originLanguage: String,
    val fileName: String
)

@Serializable
data class VocabularyContentResponse(
    val targetLanguage: String,
    val originLanguage: String,
    val fileName: String,
    val content: String,
    val wordCount: Int
)

