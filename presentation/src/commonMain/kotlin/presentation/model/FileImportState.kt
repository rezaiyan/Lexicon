package presentation.model

sealed class FileImportState {
    data object Idle : FileImportState()
    data class Loading(val progress: Int = 0, val total: Int = 0) : FileImportState()
    data class Success(val count: Int) : FileImportState()
    data class Error(val message: String, val errorType: ErrorType = ErrorType.UNKNOWN) : FileImportState() {
        enum class ErrorType {
            UNSUPPORTED_FORMAT,
            INVALID_CONTENT,
            EMPTY_FILE,
            ALL_DUPLICATES,
            UNKNOWN
        }
    }
}


