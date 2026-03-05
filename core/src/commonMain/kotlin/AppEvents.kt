package events

sealed class VocabularyEffect {
    data class ImportSuccess(val count: Int) : VocabularyEffect()
    data class ImportError(val message: String) : VocabularyEffect()
    data class ImageImportSuccess(val count: Int) : VocabularyEffect()
    data class ImageImportError(val message: String) : VocabularyEffect()
    data object ImageImportRequiresLogin : VocabularyEffect()
    data object ReviewSessionComplete : VocabularyEffect()
    data object WordDeleted : VocabularyEffect()
}