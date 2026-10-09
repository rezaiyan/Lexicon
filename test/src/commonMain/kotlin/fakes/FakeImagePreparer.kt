package fakes

import core.common.Try
import domain.word.add.service.IImagePreparer

class FakeImagePreparer : IImagePreparer {
    /** null = pass the image through unchanged. */
    var result: Try<ByteArray>? = null

    var lastQuarterTurns: Int? = null

    override suspend fun prepare(image: ByteArray, quarterTurns: Int): Try<ByteArray> {
        lastQuarterTurns = quarterTurns
        return result ?: Try.success(image)
    }
}
