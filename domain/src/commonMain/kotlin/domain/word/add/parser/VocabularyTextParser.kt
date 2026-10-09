package domain.word.add.parser

import core.common.fold
import core.error.DomainError
import core.error.DomainError.AddWords.InvalidDraft.Reason
import domain.word.add.model.WordDraft

data class ParseReport(val drafts: List<WordDraft>, val rejected: List<RejectedLine>)

data class RejectedLine(val lineNumber: Int, val raw: String, val reason: RejectReason)

enum class RejectReason { MissingTranslation, TooLong, Malformed }

/**
 * Turns free-form vocabulary text (files, pasted lists, legacy AI output) into [WordDraft]s.
 *
 * Supports comma / tab / semicolon / pipe separated lines, RFC-4180 quoting (fields may contain the
 * separator, escaped quotes and line breaks), a byte order mark, Windows line endings, `#` and `//`
 * comments, a header row, and the legacy single-line `word,translation;word,translation` format.
 * Columns: term, translation, optional note (any extra columns are folded into the note).
 * Pure: no I/O, never throws.
 */
object VocabularyTextParser {

    private const val QUOTE = '"'
    private val Candidates = listOf('\t', ',', ';', '|')
    private val HeaderWords = setOf(
        "word", "words", "term", "front", "original", "source", "vocabulary",
        "translation", "meaning", "back", "definition", "target", "description", "note", "notes",
    )

    fun parse(text: String): ParseReport {
        val normalized = text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.isBlank()) return ParseReport(emptyList(), emptyList())

        val delimiter = detectDelimiter(normalized)
        val records = readRecords(normalized, delimiter)
            .filterNot { it.raw.isBlank() || it.isComment() }
            .flatMap { it.expandLegacy(delimiter) }
            .map { it.splitByOwnDelimiter() }
            .let { if (it.firstOrNull()?.isHeader() == true) it.drop(1) else it }

        val drafts = mutableListOf<WordDraft>()
        val rejected = mutableListOf<RejectedLine>()
        records.forEach { record ->
            val fields = record.fields.map { it.trim() }
            WordDraft.of(
                term = fields.first(),
                translation = fields.getOrElse(1) { "" },
                note = fields.drop(2).joinToString(noteJoiner(record.delimiter ?: delimiter)),
            ).fold(
                onSuccess = { drafts += it },
                onFailure = { error ->
                    rejected += RejectedLine(record.lineNumber, record.raw.trim(), error.toReason())
                },
            )
        }
        return ParseReport(drafts, rejected)
    }

    private fun detectDelimiter(text: String): Char {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
            .take(50)
            .toList()
        val threshold = (lines.size + 1) / 2
        return Candidates.firstOrNull { candidate -> lines.count { candidate in it } >= threshold } ?: ','
    }

    private fun noteJoiner(delimiter: Char) = when (delimiter) {
        ',' -> ", "
        '\t' -> " "
        else -> " $delimiter "
    }

    private class Record(
        val lineNumber: Int,
        val raw: String,
        val fields: List<String>,
        val quoted: Boolean,
        /** Set when the line used its own delimiter instead of the file's. */
        val delimiter: Char? = null,
    ) {
        fun isComment(): Boolean = raw.trimStart().let { it.startsWith("#") || it.startsWith("//") }

        fun isHeader(): Boolean = fields.size >= 2 &&
            fields.take(2).all { it.trim().lowercase() in HeaderWords }

        /** `a,b;c,d` on one line: the old app and AI format used `;` between entries. */
        fun expandLegacy(delimiter: Char): List<Record> {
            if (delimiter != ',' || quoted || ';' !in raw) return listOf(this)
            val segments = raw.split(';').filter { it.isNotBlank() }
            if (segments.size < 2 || segments.any { ',' !in it }) return listOf(this)
            return segments.map { Record(lineNumber, it, it.split(','), quoted = false) }
        }

        /** A line without the file's delimiter (`a; b; c` in a comma file) is split by the first one it has. */
        fun splitByOwnDelimiter(): Record {
            if (fields.size > 1 || quoted) return this
            val own = Candidates.firstOrNull { it in raw } ?: return this
            return Record(lineNumber, raw, raw.split(own), quoted = false, delimiter = own)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
    private fun readRecords(
        text: String,
        delimiter: Char,
        quoting: Boolean = true,
        firstLine: Int = 1,
    ): List<Record> {
        val records = mutableListOf<Record>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var quoted = false
        var line = firstLine
        var recordLine = firstLine
        var recordStart = 0
        var atFieldStart = true
        var i = 0

        // A stray opening quote would swallow everything up to the next quote: from that line on, quotes are text.
        fun readRestLiterally(): List<Record> =
            records + readRecords(text.substring(recordStart), delimiter, quoting = false, firstLine = recordLine)

        fun endRecord(end: Int) {
            fields.add(field.toString())
            records += Record(recordLine, text.substring(recordStart, end), fields, quoted)
            fields = mutableListOf()
            field.clear()
            quoted = false
            atFieldStart = true
        }

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == QUOTE && text.getOrNull(i + 1) == QUOTE -> {
                    field.append(QUOTE)
                    i++
                }
                inQuotes && c == QUOTE -> {
                    if (!text.closesFieldAt(i + 1, delimiter)) return readRestLiterally()
                    inQuotes = false
                }
                inQuotes -> {
                    if (c == '\n') line++
                    field.append(c)
                }
                quoting && atFieldStart && c == QUOTE -> {
                    inQuotes = true
                    quoted = true
                    atFieldStart = false
                }
                c == delimiter -> {
                    fields.add(field.toString())
                    field.clear()
                    atFieldStart = true
                }
                c == '\n' -> {
                    endRecord(i)
                    line++
                    recordLine = line
                    recordStart = i + 1
                }
                else -> {
                    // Leading spaces before an opening quote (`a, "b"`) still count as field start.
                    if (!(atFieldStart && c == ' ')) atFieldStart = false
                    field.append(c)
                }
            }
            i++
        }
        if (inQuotes) return readRestLiterally()
        if (recordStart < text.length) endRecord(text.length)
        return records
    }

    /** RFC 4180: a closing quote must end the field. */
    private fun String.closesFieldAt(index: Int, delimiter: Char): Boolean {
        var next = index
        while (next < length && this[next] == ' ') next++
        return next == length || this[next] == delimiter || this[next] == '\n'
    }

    private fun Throwable.toReason(): RejectReason = when ((this as? DomainError.AddWords.InvalidDraft)?.reason) {
        Reason.BlankTranslation -> RejectReason.MissingTranslation
        Reason.TooLong -> RejectReason.TooLong
        else -> RejectReason.Malformed
    }
}
