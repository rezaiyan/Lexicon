package domain.word.add.model

import core.common.Try
import core.error.DomainError
import core.error.DomainError.AddWords.InvalidDraft.Reason

/**
 * A word the user (or a source like a file, photo or AI) wants to add, validated and normalized.
 * [of] is the only way to build one, so every draft in the pipeline is already clean.
 */
@ConsistentCopyVisibility
data class WordDraft private constructor(
    val term: String,
    val translation: String,
    val note: String,
) {
    /** Case-insensitive identity used to spot duplicates. */
    val key: DraftKey get() = DraftKey(term.lowercase(), translation.lowercase())

    companion object {
        const val MAX_TERM_LENGTH = 200
        const val MAX_NOTE_LENGTH = 1_000

        fun of(term: String, translation: String, note: String = ""): Try<WordDraft> {
            val cleanTerm = term.normalizeInline()
            val cleanTranslation = translation.normalizeInline()
            val cleanNote = note.normalizeMultiline()
            val reason = when {
                cleanTerm.isEmpty() -> Reason.BlankTerm
                cleanTranslation.isEmpty() -> Reason.BlankTranslation
                cleanTerm.length > MAX_TERM_LENGTH ||
                    cleanTranslation.length > MAX_TERM_LENGTH ||
                    cleanNote.length > MAX_NOTE_LENGTH -> Reason.TooLong
                else -> null
            }
            return if (reason == null) {
                Try.success(WordDraft(cleanTerm, cleanTranslation, cleanNote))
            } else {
                Try.failure(DomainError.AddWords.InvalidDraft(reason))
            }
        }
    }
}

data class DraftKey(val term: String, val translation: String)

private const val BYTE_ORDER_MARK = '﻿'
private val Whitespace = Regex("\\s+")
private val InlineSpace = Regex("[ \\t]+")

private fun Char.isStrippedControl(keepLineBreaks: Boolean): Boolean =
    this == BYTE_ORDER_MARK || (isISOControl() && !(keepLineBreaks && this == '\n') && this != '\t')

private fun String.normalizeInline(): String =
    filterNot { it.isStrippedControl(keepLineBreaks = false) && !it.isWhitespace() }
        .replace(Whitespace, " ")
        .trim()

private fun String.normalizeMultiline(): String =
    replace("\r\n", "\n").replace('\r', '\n')
        .filterNot { it.isStrippedControl(keepLineBreaks = true) }
        .lines()
        .joinToString("\n") { it.replace(InlineSpace, " ").trim() }
        .trim()
