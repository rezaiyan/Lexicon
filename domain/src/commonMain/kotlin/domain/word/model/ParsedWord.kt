package domain.word.model

data class ParsedWord(
    val word: String,
    val translation: String,
    val description: String = "",
)
