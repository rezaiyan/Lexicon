package domain.word.usecase

import core.common.Try
import core.common.UseCase
import domain.word.model.Word

/**
 * Exports words as text that the file import reads back.
 *
 * Without tags: the compact legacy format `word1,translation1;word2,translation2,note2`.
 * With tags ([Params.tagNames] set): CSV, one word per line under a `word,translation,note,tags` header;
 * the tags field lists the word's tag names separated by `;`.
 */
class ExportWordsUseCase : UseCase<ExportWordsUseCase.Params, String> {

    /** [tagNames] maps tag id to name; null exports without tags. */
    data class Params(val words: List<Word>, val tagNames: Map<Long, String>? = null)

    override suspend operator fun invoke(params: Params): Try<String> = Try {
        when {
            params.words.isEmpty() -> ""
            params.tagNames == null -> compact(params.words)
            else -> csvWithTags(params.words, params.tagNames)
        }
    }

    private fun compact(words: List<Word>): String =
        words.joinToString(";") { word ->
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

    private fun csvWithTags(words: List<Word>, tagNames: Map<Long, String>): String {
        val rows = words.map { word ->
            val tags = word.tagIds.mapNotNull { tagNames[it]?.asTagField() }.filter { it.isNotEmpty() }
            listOf(word.originalWord, word.translation, word.description, tags.joinToString(TAG_SEPARATOR))
                .joinToString(",") { it.csvField() }
        }
        return (listOf(HEADER) + rows).joinToString("\n")
    }

    /** The import splits tags on `;`, `,` and `|`, so a name can't carry them. */
    private fun String.asTagField(): String = replace(TagBreakers, " ").replace(Whitespace, " ").trim()

    private fun String.csvField(): String =
        if (any { it in QuotedChars }) "\"" + replace("\"", "\"\"") + "\"" else this

    private companion object {
        const val HEADER = "word,translation,note,tags"
        const val TAG_SEPARATOR = ";"
        const val QuotedChars = ",;\"\n\r\t|"
        val TagBreakers = Regex("[;,|]")
        val Whitespace = Regex("\\s+")
    }
}
