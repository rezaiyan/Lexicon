package feature.aiimport.model

sealed interface AiWordImportEffect {
    data class ImportSuccess(val count: Int) : AiWordImportEffect
    data object Dismiss : AiWordImportEffect
}
