package presentation.ui.components.imports


sealed interface ImportEffect {
    data class FileImportSuccessful(val count: Int) : ImportEffect
    data class ImageImportSuccessful(val count: Int) : ImportEffect
    data class Error(val message: String = "") : ImportEffect
}
