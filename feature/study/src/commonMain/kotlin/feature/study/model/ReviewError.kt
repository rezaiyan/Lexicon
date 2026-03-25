package feature.study.model

sealed class ReviewError {
    data object Network : ReviewError()
    data class Unknown(val message: String) : ReviewError()
}
