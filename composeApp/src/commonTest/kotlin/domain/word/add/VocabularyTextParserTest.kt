package domain.word.add

import domain.word.add.parser.RejectReason
import domain.word.add.parser.VocabularyTextParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VocabularyTextParserTest {

    private fun terms(text: String) = VocabularyTextParser.parse(text).drafts.map { it.term to it.translation }

    @Test
    fun `parse comma separated lines`() {
        assertEquals(
            listOf("Hund" to "dog", "Katze" to "cat"),
            terms("Hund,dog\nKatze,cat"),
        )
    }

    @Test
    fun `parse third column becomes note`() {
        val draft = VocabularyTextParser.parse("Hund,dog,a pet").drafts.single()

        assertEquals("a pet", draft.note)
    }

    @Test
    fun `parse extra commas stay in the note instead of being dropped`() {
        val draft = VocabularyTextParser.parse("Hund,dog,loyal, friendly, furry").drafts.single()

        assertEquals("loyal, friendly, furry", draft.note)
    }

    @Test
    fun `parse quoted fields keep commas and escaped quotes`() {
        val draft = VocabularyTextParser.parse("\"gehen\",\"to go, to walk\",\"say \"\"hi\"\"\"").drafts.single()

        assertEquals("gehen", draft.term)
        assertEquals("to go, to walk", draft.translation)
        assertEquals("say \"hi\"", draft.note)
    }

    @Test
    fun `parse quoted field may span lines`() {
        val draft = VocabularyTextParser.parse("Hund,dog,\"line one\nline two\"").drafts.single()

        assertEquals("line one\nline two", draft.note)
    }

    @Test
    fun `parse tab separated keeps commas inside fields`() {
        assertEquals(
            listOf("gehen" to "to go, to walk"),
            terms("gehen\tto go, to walk"),
        )
    }

    @Test
    fun `parse semicolon separated file`() {
        assertEquals(
            listOf("Hund" to "dog", "Katze" to "cat"),
            terms("Hund;dog\nKatze;cat"),
        )
    }

    @Test
    fun `parse pipe separated file`() {
        assertEquals(listOf("Hund" to "dog"), terms("Hund | dog"))
    }

    @Test
    fun `parse legacy single line format with semicolon records`() {
        assertEquals(
            listOf("Hund" to "dog", "Katze" to "cat", "Maus" to "mouse"),
            terms("Hund,dog;Katze,cat;Maus,mouse"),
        )
    }

    @Test
    fun `parse semicolon inside a translation is not treated as a record break`() {
        assertEquals(
            listOf("gehen" to "to go; to walk"),
            terms("gehen,to go; to walk"),
        )
    }

    @Test
    fun `parse strips byte order mark and windows line endings`() {
        assertEquals(
            listOf("Hund" to "dog", "Katze" to "cat"),
            terms("﻿Hund,dog\r\nKatze,cat\r\n"),
        )
    }

    @Test
    fun `parse skips a header row`() {
        assertEquals(listOf("Hund" to "dog"), terms("Word,Translation,Description\nHund,dog,pet"))
    }

    @Test
    fun `parse skips comments and blank lines`() {
        assertEquals(listOf("Hund" to "dog"), terms("# my list\n\n// note\nHund,dog\n\n"))
    }

    @Test
    fun `parse reports lines without translation with their line number`() {
        val report = VocabularyTextParser.parse("Hund,dog\nKatze\nMaus,")

        assertEquals(listOf("Hund" to "dog"), report.drafts.map { it.term to it.translation })
        assertEquals(listOf(2, 3), report.rejected.map { it.lineNumber })
        assertTrue(report.rejected.all { it.reason == RejectReason.MissingTranslation })
        assertEquals("Katze", report.rejected.first().raw)
    }

    @Test
    fun `parse reports too long entries`() {
        val report = VocabularyTextParser.parse("${"a".repeat(500)},dog")

        assertEquals(RejectReason.TooLong, report.rejected.single().reason)
    }

    @Test
    fun `parse empty input returns empty report`() {
        val report = VocabularyTextParser.parse("  \n \n")

        assertTrue(report.drafts.isEmpty())
        assertTrue(report.rejected.isEmpty())
    }

    @Test
    fun `parse splits a line by its own delimiter when it lacks the file delimiter`() {
        val report = VocabularyTextParser.parse("Hund,dog\nder Vertrag; contract; legal agreement\nKatze,cat")

        assertEquals(listOf("Hund", "der Vertrag", "Katze"), report.drafts.map { it.term })
        assertEquals("contract", report.drafts[1].translation)
        assertEquals("legal agreement", report.drafts[1].note)
        assertTrue(report.rejected.isEmpty())
    }

    @Test
    fun `parse keeps duplicates so the caller can report them`() {
        assertEquals(2, VocabularyTextParser.parse("Hund,dog\nhund,Dog").drafts.size)
    }

    @Test
    fun `parse an unclosed opening quote does not swallow the lines after it`() {
        val report = VocabularyTextParser.parse("Hund,dog\n\"Katze,cat\nMaus,mouse\n\"Vogel\",bird")

        assertEquals(
            listOf("Hund" to "dog", "\"Katze" to "cat", "Maus" to "mouse", "\"Vogel\"" to "bird"),
            report.drafts.map { it.term to it.translation },
        )
    }
}
