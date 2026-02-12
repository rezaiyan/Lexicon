package events

sealed class VocabularyEvent {
    data class ImportSuccess(val count: Int) : VocabularyEvent()
    data class ImportError(val message: String) : VocabularyEvent()
    data class ImageImportSuccess(val count: Int) : VocabularyEvent()
    data class ImageImportError(val message: String) : VocabularyEvent()
    data object ImageImportRequiresLogin : VocabularyEvent()
    data object ReviewSessionComplete : VocabularyEvent()
}