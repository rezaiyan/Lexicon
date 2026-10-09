package domain.word.add

import core.common.exceptionOrNull
import core.common.getOrThrow
import core.error.DomainError
import domain.word.add.model.WordFile
import domain.word.add.usecase.ParseWordFileUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParseWordFileUseCaseTest {

    private val useCase = ParseWordFileUseCase()

    private suspend fun parse(bytes: ByteArray, name: String? = "words.txt") = useCase(WordFile(name, bytes))

    @Test
    fun `invoke parses utf8 content regardless of file name`() = runTest {
        val report = parse("Hund,dog\nKatze,cat".encodeToByteArray(), name = "msf:1234").getOrThrow()

        assertEquals(listOf("Hund", "Katze"), report.drafts.map { it.term })
    }

    @Test
    fun `invoke decodes utf16 little endian files exported by spreadsheets`() = runTest {
        val text = "Straße\tstreet\n"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            text.flatMap { listOf((it.code and 0xFF).toByte(), (it.code shr 8).toByte()) }.toByteArray()

        val report = parse(bytes, name = "export.csv").getOrThrow()

        assertEquals("Straße" to "street", report.drafts.single().let { it.term to it.translation })
    }

    @Test
    fun `invoke decodes utf16 big endian`() = runTest {
        val text = "Hund,dog"
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) +
            text.flatMap { listOf((it.code shr 8).toByte(), (it.code and 0xFF).toByte()) }.toByteArray()

        assertEquals("Hund", parse(bytes).getOrThrow().drafts.single().term)
    }

    @Test
    fun `invoke rejects files over the size limit`() = runTest {
        val error = parse(ByteArray(ParseWordFileUseCase.MAX_FILE_BYTES + 1) { 'a'.code.toByte() }).exceptionOrNull()

        assertIs<DomainError.AddWords.FileTooLarge>(error)
    }

    @Test
    fun `invoke rejects binary files`() = runTest {
        val error = parse(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x14, 0x00), name = "words.xlsx").exceptionOrNull()

        assertIs<DomainError.AddWords.UnsupportedFile>(error)
    }

    @Test
    fun `invoke with blank file fails with EmptyInput`() = runTest {
        assertIs<DomainError.AddWords.EmptyInput>(parse(" \n ".encodeToByteArray()).exceptionOrNull())
    }

    @Test
    fun `invoke with no usable line fails with NothingRecognized`() = runTest {
        assertIs<DomainError.AddWords.NothingRecognized>(parse("just some words\nno pairs".encodeToByteArray()).exceptionOrNull())
    }

    @Test
    fun `invoke keeps rejected lines next to the good ones`() = runTest {
        val report = parse("Hund,dog\nKatze".encodeToByteArray()).getOrThrow()

        assertEquals(1, report.drafts.size)
        assertEquals(2, report.rejected.single().lineNumber)
    }
}
