package domain.collection.model

data class VocabularyCollection(
    val targetLanguage: String,
    val originLanguage: String,
    val title: String,
    val fileName: String,
    val path: String
)

data class CollectionContent(
    val targetLanguage: String,
    val originLanguage: String,
    val fileName: String,
    val content: String,
    val wordCount: Int
)
