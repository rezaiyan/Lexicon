package presentation.model

sealed class ImportState {
    data object Idle : ImportState()
    data object Loading : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}