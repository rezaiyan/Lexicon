package data.word.add

import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrThrow
import core.error.DomainError
import domain.word.add.service.ImageLimits
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePreparerTest {

    private val photo = ByteArray(ImageLimits.MIN_UPLOAD_BYTES * 2)

    private fun preparer(encode: (ByteArray, Int, Float) -> ByteArray?) =
        ImagePreparer(UnconfinedTestDispatcher()) { bytes, edge, quality, _ -> encode(bytes, edge, quality) }

    private suspend fun ImagePreparer.failure() = (prepare(photo) as Try.Failure).exceptionOrNull()

    @Test
    fun `prepare when photo is tiny reports it unreadable without encoding`() = runTest {
        var encoded = false
        val result = preparer { _, _, _ -> encoded = true; ByteArray(1) }.prepare(ByteArray(10))

        assertEquals(DomainError.AddWords.ImageUnreadable, (result as Try.Failure).exceptionOrNull())
        assertEquals(false, encoded)
    }

    @Test
    fun `prepare rotates every encoding by the requested quarter turns`() = runTest {
        val turns = mutableListOf<Int>()
        val preparer = ImagePreparer(UnconfinedTestDispatcher()) { _, _, _, quarterTurns ->
            turns += quarterTurns
            ByteArray(if (turns.size < 2) ImageLimits.MAX_UPLOAD_BYTES + 1 else 1)
        }

        preparer.prepare(photo, quarterTurns = 3).getOrThrow()

        assertEquals(listOf(3, 3), turns)
    }

    @Test
    fun `prepare returns the first encoding that fits`() = runTest {
        val fits = ByteArray(ImageLimits.MAX_UPLOAD_BYTES)

        assertEquals(fits, preparer { _, _, _ -> fits }.prepare(photo).getOrThrow())
    }

    @Test
    fun `prepare shrinks size and quality until the photo fits`() = runTest {
        val attempts = mutableListOf<Pair<Int, Float>>()
        val result = preparer { _, edge, quality ->
            attempts += edge to quality
            ByteArray(if (attempts.size < 3) ImageLimits.MAX_UPLOAD_BYTES + 1 else 1)
        }.prepare(photo)

        assertEquals(1, result.getOrThrow().size)
        assertEquals(listOf(ImageLimits.MAX_EDGE_PX to 0.85f, ImageLimits.MAX_EDGE_PX to 0.7f, 1_600 to 0.7f), attempts)
    }

    @Test
    fun `prepare when nothing fits reports the photo too large`() = runTest {
        val tooBig = ByteArray(ImageLimits.MAX_UPLOAD_BYTES + 1)

        assertEquals(
            DomainError.AddWords.ImageTooLarge(ImageLimits.MAX_UPLOAD_BYTES),
            preparer { _, _, _ -> tooBig }.failure(),
        )
    }

    @Test
    fun `prepare when the photo cannot be decoded reports it unreadable`() = runTest {
        assertEquals(DomainError.AddWords.ImageUnreadable, preparer { _, _, _ -> null }.failure())
    }
}
