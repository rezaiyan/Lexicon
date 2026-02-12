package presentation.ui.components.imports

sealed class ImportFileState {
    data object Idle : ImportFileState()
    data object Loading : ImportFileState()
    data class Success(val count: Int) : ImportFileState()
    data class Error(val message: String) : ImportFileState()
}


