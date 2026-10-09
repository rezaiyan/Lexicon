package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.error.DomainError
import domain.word.add.model.WordFile
import domain.word.add.parser.ParseReport
import domain.word.add.parser.TextDecoder
import domain.word.add.parser.VocabularyTextParser

/**
 * Reads a vocabulary file into reviewable drafts. Any text file works (.txt, .csv, .tsv, …);
 * the format is detected from content, never from the file name.
 */
class ParseWordFileUseCase : UseCase<WordFile, ParseReport> {

    override suspend fun invoke(params: WordFile): Try<ParseReport> {
        if (params.bytes.size > MAX_FILE_BYTES) {
            return Try.failure(DomainError.AddWords.FileTooLarge(MAX_FILE_BYTES))
        }
        val text = TextDecoder.decode(params.bytes)
            ?: return Try.failure(DomainError.AddWords.UnsupportedFile)
        if (text.isBlank()) return Try.failure(DomainError.AddWords.EmptyInput)

        val report = VocabularyTextParser.parse(text)
        return if (report.drafts.isEmpty()) {
            Try.failure(DomainError.AddWords.NothingRecognized)
        } else {
            Try.success(report)
        }
    }

    companion object {
        const val MAX_FILE_BYTES = 1_000_000
    }
}
