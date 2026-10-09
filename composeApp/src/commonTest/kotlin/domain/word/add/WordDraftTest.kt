package domain.word.add

import core.common.exceptionOrNull
import core.common.getOrNull
import core.error.DomainError
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class WordDraftTest {

    @Test
    fun `of when input has padding and repeated spaces normalizes it`() {
        val draft = WordDraft.of("  der   Hund ", "\tthe dog\n", "  a pet  ").getOrNull()

        assertEquals("der Hund", draft?.term)
        assertEquals("the dog", draft?.translation)
        assertEquals("a pet", draft?.note)
    }

    @Test
    fun `of when term starts with byte order mark strips it`() {
        val draft = WordDraft.of("﻿Hund", "dog").getOrNull()

        assertEquals("Hund", draft?.term)
    }

    @Test
    fun `of keeps separators that used to break the csv round trip`() {
        val draft = WordDraft.of("gehen", "to go; to walk, to stroll").getOrNull()

        assertEquals("to go; to walk, to stroll", draft?.translation)
    }

    @Test
    fun `of when term is blank fails with BlankTerm`() {
        val error = WordDraft.of("   ", "dog").exceptionOrNull()

        assertIs<DomainError.AddWords.InvalidDraft>(error)
        assertEquals(DomainError.AddWords.InvalidDraft.Reason.BlankTerm, error.reason)
    }

    @Test
    fun `of when translation is blank fails with BlankTranslation`() {
        val error = WordDraft.of("Hund", "").exceptionOrNull()

        assertIs<DomainError.AddWords.InvalidDraft>(error)
        assertEquals(DomainError.AddWords.InvalidDraft.Reason.BlankTranslation, error.reason)
    }

    @Test
    fun `of when term exceeds limit fails with TooLong`() {
        val error = WordDraft.of("a".repeat(WordDraft.MAX_TERM_LENGTH + 1), "x").exceptionOrNull()

        assertIs<DomainError.AddWords.InvalidDraft>(error)
        assertEquals(DomainError.AddWords.InvalidDraft.Reason.TooLong, error.reason)
    }

    @Test
    fun `of removes control characters but keeps line breaks in note`() {
        val draft = WordDraft.of("Hu\u0000nd", "dog", "line one\nline two\u0007").getOrNull()

        assertEquals("Hund", draft?.term)
        assertEquals("line one\nline two", draft?.note)
    }

    @Test
    fun `key ignores case so drafts differing only by case collide`() {
        val a = WordDraft.of("Hund", "Dog").getOrNull()
        val b = WordDraft.of("hund", "dog").getOrNull()

        assertEquals(a?.key, b?.key)
    }

    @Test
    fun `key differs when translation differs`() {
        val a = WordDraft.of("Bank", "bank").getOrNull()
        val b = WordDraft.of("Bank", "bench").getOrNull()

        assertNotEquals(a?.key, b?.key)
    }

    @Test
    fun `LanguagePair rejects identical languages`() {
        assertFailsWith<IllegalArgumentException> { LanguagePair(Language.GERMAN, Language.GERMAN) }
    }

    @Test
    fun `LanguagePair orNull returns null for identical languages`() {
        assertNull(LanguagePair.orNull(Language.GERMAN, Language.GERMAN))
    }
}
