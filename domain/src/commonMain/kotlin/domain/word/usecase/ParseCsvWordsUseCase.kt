package domain.word.usecase

import domain.word.model.ParsedWord

class ParseCsvWordsUseCase {

    operator fun invoke(csvText: String): List<ParsedWord> {
        return csvText.trim()
            .split(Regex("[;\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split(",", limit = 3).map { it.trim() }
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    ParsedWord(
                        word = parts[0],
                        translation = parts[1],
                        description = if (parts.size > 2) parts[2] else "",
                    )
                } else null
            }
    }
}
