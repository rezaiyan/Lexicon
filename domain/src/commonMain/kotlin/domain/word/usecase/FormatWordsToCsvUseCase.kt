package domain.word.usecase

import domain.word.model.ParsedWord

class FormatWordsToCsvUseCase {

    operator fun invoke(words: List<ParsedWord>): String {
        return words.joinToString("\n") { word ->
            if (word.description.isNotBlank()) "${word.word},${word.translation},${word.description}"
            else "${word.word},${word.translation}"
        }
    }
}
