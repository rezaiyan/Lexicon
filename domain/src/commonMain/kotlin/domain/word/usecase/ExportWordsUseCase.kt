package domain.word.usecase

import domain.word.model.Word

/**
 * Use case for exporting words to text format
 * Format: word1,translation1;word2,translation2;word3,translation3
 * This format matches the import format for easy re-import
 */
class ExportWordsUseCase {
    operator fun invoke(words: List<Word>): String {
        if (words.isEmpty()) return ""
        
        return words.joinToString(";") { word ->
            buildString {
                append(word.originalWord)
                append(",")
                append(word.translation)
                
                if (word.description.isNotBlank()) {
                    append(",")
                    append(word.description)
                }
            }
        }
    }
}

