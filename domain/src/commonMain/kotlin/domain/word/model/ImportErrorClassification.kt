package domain.word.model

sealed interface ImportErrorClassification {
    data object NetworkError : ImportErrorClassification
    data object EmptyContent : ImportErrorClassification
    data object GenericError : ImportErrorClassification
}
