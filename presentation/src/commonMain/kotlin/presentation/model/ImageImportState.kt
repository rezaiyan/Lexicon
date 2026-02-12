package presentation.model

sealed class ImageImportState {
    data object Idle : ImageImportState()
    data object Loading : ImageImportState()
    data class Success(val count: Int) : ImageImportState()
    data class Error(val message: String) : ImageImportState()
}

