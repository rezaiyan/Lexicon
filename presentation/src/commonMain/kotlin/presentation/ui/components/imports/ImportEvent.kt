package presentation.ui.components.imports


sealed interface ImportEvent {
    data class WordAddedSuccessfully(val count: Int) : ImportEvent
    data class FileImportSuccessful(val count: Int) : ImportEvent
    data class ImageImportSuccessful(val count: Int) : ImportEvent
    data class Error(val message: String = "") : ImportEvent
}
