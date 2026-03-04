package domain.word.usecase

import core.common.Try
import core.common.UseCase
import domain.word.model.Word

/**
 * Use case for exporting words to text format
 * Format: word1,translation1;word2,translation2;word3,translation3
 * This format matches the import format for easy re-import
 */
class ExportWordsUseCase : UseCase<List<Word>, String> {
    override suspend operator fun invoke(params: List<Word>): Try<String> = Try {
        if (params.isEmpty()) return@Try ""

        params.joinToString(";") { word ->
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
