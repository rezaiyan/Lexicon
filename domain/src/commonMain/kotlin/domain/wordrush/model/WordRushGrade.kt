package domain.wordrush.model

sealed class WordRushGrade(val code: String) {
    data object S : WordRushGrade("S")
    data object A : WordRushGrade("A")
    data object B : WordRushGrade("B")
    data object C : WordRushGrade("C")
    data object D : WordRushGrade("D")

    companion object {
        fun fromAccuracy(accuracy: Float): WordRushGrade = when {
            accuracy >= 0.9f -> S
            accuracy >= 0.8f -> A
            accuracy >= 0.6f -> B
            accuracy >= 0.4f -> C
            else -> D
        }

        fun fromCode(code: String): WordRushGrade = when (code) {
            "S" -> S
            "A" -> A
            "B" -> B
            "C" -> C
            else -> D
        }
    }
}
