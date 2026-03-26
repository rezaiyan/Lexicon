package domain.word.usecase

import domain.word.model.ParsedWord

class FormatWordsToCsvUseCase {

    operator fun invoke(words: List<ParsedWord>): String {
        return words.joinToString("\n") { word ->
            formatSingle(word)
        }
    }

    operator fun invoke(word: ParsedWord): String = formatSingle(word)

    private fun formatSingle(word: ParsedWord): String {
        val w = word.word.trim().replace(",", " ")
        val t = word.translation.trim().replace(",", " ")
        val d = word.description.trim().replace(",", " ")
        return if (d.isNotBlank()) "$w,$t,$d" else "$w,$t"
    }
}
